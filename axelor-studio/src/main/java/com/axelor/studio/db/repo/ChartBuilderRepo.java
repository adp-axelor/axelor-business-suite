/**
 * Axelor Business Solutions
 *
 * Copyright (C) 2016 Axelor (<http://axelor.com>).
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
package com.axelor.studio.db.repo;

import javax.validation.ValidationException;
import javax.xml.bind.JAXBException;

import com.axelor.exception.AxelorException;
import com.axelor.i18n.I18n;
import com.axelor.meta.db.MetaView;
import com.axelor.meta.db.repo.MetaViewRepository;
import com.axelor.studio.db.ChartBuilder;
import com.axelor.studio.service.builder.ChartBuilderService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;

public class ChartBuilderRepo extends ChartBuilderRepository {

	@Inject
	private MetaViewRepository metaViewRepo;
	
	@Inject
	private ChartBuilderService chartBuilderService;

	@Override
	public ChartBuilder save(ChartBuilder chartBuilder) throws ValidationException {

		if (chartBuilder.getName().contains(" ")) {
			throw new ValidationException(
					I18n.get("Name must not contains space"));
		}
		
		chartBuilder = super.save(chartBuilder);
		
		try {
			MetaView metaView = chartBuilderService.build(chartBuilder);
			if (metaView != null) {
				chartBuilder.setMetaViewGenerated(metaView);
			}
		} catch (AxelorException | JAXBException e) {
			throw new ValidationException(e.getMessage());
		}

		return super.save(chartBuilder);
	}

	@Override
	@Transactional
	public void remove(ChartBuilder chartBuilder) {

		MetaView metaView = chartBuilder.getMetaViewGenerated();
		if (metaView != null) {
			metaViewRepo.remove(metaView);
		}

		super.remove(chartBuilder);
	}

}