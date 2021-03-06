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
package com.axelor.apps.supplychain.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDate;

import com.axelor.apps.account.db.Budget;
import com.axelor.apps.account.db.BudgetDistribution;
import com.axelor.apps.account.db.BudgetLine;
import com.axelor.apps.account.db.repo.BudgetDistributionRepository;
import com.axelor.apps.account.db.repo.InvoiceRepository;
import com.axelor.apps.account.service.BudgetService;
import com.axelor.apps.base.service.administration.GeneralService;
import com.axelor.inject.Beans;
import com.google.inject.Inject;

public class BudgetSupplychainService extends BudgetService{
	
	@Inject 
	GeneralService generalservice;
	
	@Override
	public List<BudgetLine> updateLines(Budget budget){
		if(budget.getBudgetLineList() != null && !budget.getBudgetLineList().isEmpty()){
			for (BudgetLine budgetLine : budget.getBudgetLineList()) {
				budgetLine.setAmountCommitted(BigDecimal.ZERO);
				budgetLine.setAmountRealized(BigDecimal.ZERO);
			}
			List<Integer> statusList= new ArrayList<Integer>();
			if (generalservice.getGeneral().getManageBudget()){
				if(!generalservice.getGeneral().getBudgetStatusSelect().isEmpty()){
					String str= generalservice.getGeneral().getBudgetStatusSelect();
					String[] numbers = str.split(", ");
					for(int c = 0; c < numbers.length; c++) 
						statusList.add(Integer.parseInt(numbers[c]));
				}
			}
			List<BudgetDistribution> budgetDistributionList = null;
			if(!statusList.isEmpty()){
				budgetDistributionList = Beans.get(BudgetDistributionRepository.class).all().filter("self.budget.id = ?1 AND (self.purchaseOrderLine.purchaseOrder.statusSelect in (?2) OR ?3 is null)", budget.getId(),statusList,statusList).fetch();
				for (BudgetDistribution budgetDistribution : budgetDistributionList) {
					LocalDate orderDate = budgetDistribution.getPurchaseOrderLine().getPurchaseOrder().getOrderDate();
					if(orderDate != null){
						for (BudgetLine budgetLine : budget.getBudgetLineList()) {
							LocalDate fromDate = budgetLine.getFromDate();
							LocalDate toDate = budgetLine.getToDate();
							if((fromDate.isBefore(orderDate) || fromDate.isEqual(orderDate)) && (toDate.isAfter(orderDate) || toDate.isEqual(orderDate))){
								budgetLine.setAmountCommitted(budgetLine.getAmountCommitted().add(budgetDistribution.getAmount()));
								break;
							}
						}
					}
				}
			}
			budgetDistributionList = Beans.get(BudgetDistributionRepository.class).all().filter("self.budget.id = ?1 AND (self.invoiceLine.invoice.statusSelect = ?2 OR self.invoiceLine.invoice.statusSelect = ?3)", budget.getId(), InvoiceRepository.STATUS_VALIDATED, InvoiceRepository.STATUS_VENTILATED).fetch();
			for (BudgetDistribution budgetDistribution : budgetDistributionList) {
				LocalDate orderDate = budgetDistribution.getInvoiceLine().getInvoice().getInvoiceDate();
				if(orderDate != null){
					for (BudgetLine budgetLine : budget.getBudgetLineList()) {
						LocalDate fromDate = budgetLine.getFromDate();
						LocalDate toDate = budgetLine.getToDate();
						if((fromDate.isBefore(orderDate) || fromDate.isEqual(orderDate)) && (toDate.isAfter(orderDate) || toDate.isEqual(orderDate))){
							budgetLine.setAmountRealized(budgetLine.getAmountRealized().add(budgetDistribution.getAmount()));
							break;
						}
					}
				}
			}
		}
		return budget.getBudgetLineList();
	}
}
