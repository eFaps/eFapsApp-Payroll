/*
 * Copyright 2003 - 2015 The eFaps Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.esjp.payroll;

import java.math.BigDecimal;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIFormPayroll;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.sales.document.AbstractDocument;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: $
 */
public abstract class BulkPayment_Base
    extends AbstractDocument
{

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new empty Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final CreatedDoc createdDoc = createDoc(_parameter);
        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new CreatedDoc
     * @throws EFapsException on error
     */
    public CreatedDoc createDoc(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc ret = new CreatedDoc();
        final Instance baseInst = Currency.getBaseCurrency();
        final Insert insert = new Insert(CIPayroll.BulkPayment);
        insert.add(CIPayroll.BulkPayment.Date,
                        _parameter.getParameterValue(CIFormPayroll.Payroll_BulkPaymentForm.date.name));
        insert.add(CIPayroll.BulkPayment.Name,
                        _parameter.getParameterValue(CIFormPayroll.Payroll_BulkPaymentForm.name.name));
        insert.add(CIPayroll.BulkPayment.BulkDefinitionLink,
                        _parameter.getParameterValue(CIFormPayroll.Payroll_BulkPaymentForm.bulkDefinition.name));
        insert.add(CIPayroll.BulkPayment.CrossTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.BulkPayment.CrossTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.BulkPayment.NetTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.BulkPayment.DiscountTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.BulkPayment.RateCrossTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.BulkPayment.RateNetTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.BulkPayment.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CIPayroll.BulkPayment.CurrencyId, baseInst);
        insert.add(CIPayroll.BulkPayment.RateCurrencyId, baseInst);
        insert.add(CIPayroll.BulkPayment.Rate, new Object[] { 1, 1 });
        insert.add(CIPayroll.BulkPayment.Status, Status.find(CISales.BulkPaymentStatus.Open));
        insert.execute();
        ret.setInstance(insert.getInstance());

        final Insert relinsert = new Insert(CISales.BulkPayment2Account);
        relinsert.add(CISales.BulkPayment2Account.FromLink, insert.getId());
        relinsert.add(CISales.BulkPayment2Account.ToLink,
                        _parameter.getParameterValue(CIFormPayroll.Payroll_BulkPaymentForm.account4create.name));
        relinsert.execute();

        return ret;
    }
}
