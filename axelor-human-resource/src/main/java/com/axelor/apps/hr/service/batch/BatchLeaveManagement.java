/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2017 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.hr.service.batch;

import com.axelor.apps.hr.service.batch.BatchStrategy;
import com.axelor.apps.hr.service.leave.LeaveService;
import com.axelor.apps.hr.service.leave.management.LeaveManagementService;
import com.axelor.auth.AuthUtils;
import com.axelor.db.JPA;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.axelor.apps.base.db.WeeklyPlanning;
import com.axelor.apps.hr.exception.IExceptionMessage;
import com.axelor.apps.hr.db.Employee;
import com.axelor.apps.hr.db.HrBatch;
import com.axelor.apps.hr.db.LeaveLine;
import com.axelor.apps.hr.db.LeaveManagement;
import com.axelor.apps.hr.db.LeaveReason;
import com.axelor.apps.hr.db.repo.LeaveLineRepository;
import com.axelor.apps.hr.db.repo.LeaveManagementRepository;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.IException;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.beust.jcommander.internal.Lists;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.Transactional;

public class BatchLeaveManagement extends BatchStrategy {
	
	private final Logger log = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );
	
	int total;
	int noValueAnomaly;
	int confAnomaly;
	
	protected LeaveLineRepository leaveLineRepository;
	protected LeaveManagementRepository leaveManagementRepository;
	
	@Inject
	private Provider<LeaveService> leaveServiceProvider;
	
	
	@Inject
	public BatchLeaveManagement(LeaveManagementService leaveManagementService, LeaveLineRepository leaveLineRepository, LeaveManagementRepository leaveManagementRepository) {
		
		super(leaveManagementService);
		this.leaveLineRepository = leaveLineRepository;
		this.leaveManagementRepository = leaveManagementRepository;
	}

	@Override
	protected void start() throws IllegalArgumentException, IllegalAccessException, AxelorException {
		
		super.start();
		
		if (batch.getHrBatch().getDayNumber() == null || batch.getHrBatch().getDayNumber() == BigDecimal.ZERO || batch.getHrBatch().getLeaveReason() == null)
			TraceBackService.trace(new AxelorException(I18n.get(IExceptionMessage.BATCH_MISSING_FIELD), IException.CONFIGURATION_ERROR), IException.LEAVE_MANAGEMENT, batch.getId());
		total = 0;
		noValueAnomaly = 0;
		confAnomaly = 0;
		checkPoint();

	}

	
	@Override
	protected void process() {
	
			List<Employee> employeeList = this.getEmployees(batch.getHrBatch());
			generateLeaveManagementLines(employeeList);
	}
	
	public List<Employee> getEmployees(HrBatch hrBatch){
		
		List<String> query = Lists.newArrayList();
		
		if ( !hrBatch.getEmployeeSet().isEmpty() ){
			String employeeIds = Joiner.on(',').join(  
					Iterables.transform(hrBatch.getEmployeeSet(), new Function<Employee,String>() {
			            public String apply(Employee obj) {
			                return obj.getId().toString();
			            }
			        }) ); 
			query.add("self.id IN (" + employeeIds + ")");
		}
		if ( !hrBatch.getPlanningSet().isEmpty() ){
			String planningIds = Joiner.on(',').join(  
					Iterables.transform(hrBatch.getPlanningSet(), new Function<WeeklyPlanning,String>() {
			            public String apply(WeeklyPlanning obj) {
			                return obj.getId().toString();
			            }
			        }) ); 
			
			query.add("self.planning.id IN (" + planningIds + ")");
		}
		
		List<Employee> employeeList = Lists.newArrayList();
		String liaison = query.isEmpty() ? "" : " AND";
		if (hrBatch.getCompany() != null){
			employeeList = JPA.all(Employee.class).filter(Joiner.on(" AND ").join(query) + liaison + " (EXISTS(SELECT u FROM User u WHERE :company MEMBER OF u.companySet AND self = u.employee) OR NOT EXISTS(SELECT u FROM User u WHERE self = u.employee))").bind("company", hrBatch.getCompany()).fetch();
		}
		else{
			employeeList = JPA.all(Employee.class).filter(Joiner.on(" AND ").join(query)).fetch();
		}
		
		return employeeList;
	}
	
	
	public void generateLeaveManagementLines(List<Employee> employeeList){
		
		for (Employee employee : employeeList) {
			
			try{
				createLeaveManagement(employeeRepository.find(employee.getId()));
			}
			catch(AxelorException e){
				TraceBackService.trace(e, IException.LEAVE_MANAGEMENT, batch.getId());
				incrementAnomaly();
				if (e.getcategory() == IException.NO_VALUE ){
					noValueAnomaly ++;
				}
				if (e.getcategory() == IException.CONFIGURATION_ERROR ){
					confAnomaly ++;
				}
			}
			finally {
				total ++;
				JPA.clear();
			}
		}
	}
	
	@Transactional
	public void createLeaveManagement(Employee employee) throws AxelorException{  
		
		batch = batchRepo.find(batch.getId());
		LeaveLine leaveLine = null;
		LeaveReason leaveReason = batch.getHrBatch().getLeaveReason();
		
		if(employee != null){
			leaveLine = leaveServiceProvider.get().addLeaveReasonOrCreateIt(employee, leaveReason);
			
			BigDecimal dayNumber = batch.getHrBatch().getUseWeeklyPlanningCoef() ? batch.getHrBatch().getDayNumber().multiply(employee.getPlanning().getLeaveCoef()) : batch.getHrBatch().getDayNumber();
			dayNumber = dayNumber.subtract(new BigDecimal( publicHolidayService.getImposedDayNumber(employee, batch.getHrBatch().getStartDate(), batch.getHrBatch().getEndDate()) ));
			LeaveManagement leaveManagement = leaveManagementService.createLeaveManagement(leaveLine, AuthUtils.getUser(), batch.getHrBatch().getComments(), null, batch.getHrBatch().getStartDate(), batch.getHrBatch().getEndDate(), dayNumber );
			leaveLine.setQuantity(leaveLine.getQuantity().add(dayNumber).setScale(1));
			
			leaveManagementRepository.save(leaveManagement);
			leaveLineRepository.save(leaveLine);
			updateEmployee(employee);
		}		
	}
	
	@Override
	protected void stop() {
		
		String comment = String.format(I18n.get(IExceptionMessage.BATCH_LEAVE_MANAGEMENT_ENDING_0) + '\n', total); 
		
		comment += String.format(I18n.get(IExceptionMessage.BATCH_LEAVE_MANAGEMENT_ENDING_1) + '\n', batch.getDone()); 
		
		if (confAnomaly > 0){
			comment += String.format(I18n.get(IExceptionMessage.BATCH_LEAVE_MANAGEMENT_ENDING_2) + '\n', confAnomaly); 
		}
		if (noValueAnomaly > 0){
			comment += String.format(I18n.get(IExceptionMessage.BATCH_LEAVE_MANAGEMENT_ENDING_3) + '\n', noValueAnomaly); 
		}
		
		addComment(comment);
		super.stop();
	}

}
