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
package com.axelor.apps.production.service;

import java.util.List;

import com.axelor.apps.base.db.Product;
import com.axelor.apps.production.db.BillOfMaterial;
import com.axelor.apps.sale.db.SaleOrderLine;
import com.axelor.exception.AxelorException;
import com.google.inject.persist.Transactional;

public interface BillOfMaterialService {

	static final String UNIT_MIN_CODE = "MIN";
	
	static final String UNIT_DAY_CODE = "JR";
	
	
	public List<BillOfMaterial> getBillOfMaterialList(Product product);
	
	
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public void updateProductCostPrice(BillOfMaterial billOfMaterial) throws AxelorException;
		
	
	@Transactional(rollbackOn = {AxelorException.class, Exception.class})
	public BillOfMaterial customizeBillOfMaterial(SaleOrderLine saleOrderLine);
	
	public BillOfMaterial generateNewVersion(BillOfMaterial billOfMaterial);
	
}
