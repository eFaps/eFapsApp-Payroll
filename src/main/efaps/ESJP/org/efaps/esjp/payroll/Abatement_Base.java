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
import java.text.ParseException;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormPayroll;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.common.uiform.Edit;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.sales.document.AbstractDocument;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: $
 */
@EFapsUUID("87300c22-90b2-4997-8953-e4b451731751")
@EFapsRevision("$Rev: 14839 $")
public abstract class Abatement_Base
    extends AbstractDocument
{

    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final Create create = new Create()
        {

            @Override
            protected void add2basicInsert(final Parameter _parameter,
                                           final Insert _insert)
                throws EFapsException
            {
                super.add2basicInsert(_parameter, _insert);
                _insert.add(CIERP.DocumentAbstract.Name, getDocName4Create(_parameter));
                _insert.add(CIERP.DocumentAbstract.Name, getDocName4Create(_parameter));
                _insert.add(CIPayroll.DocumentAbstract.RateCrossTotal, BigDecimal.ZERO);
                _insert.add(CIPayroll.DocumentAbstract.RateNetTotal, BigDecimal.ZERO);
                _insert.add(CIPayroll.DocumentAbstract.Rate, new Object[] { BigDecimal.ZERO, BigDecimal.ZERO });
                _insert.add(CIPayroll.DocumentAbstract.CrossTotal, BigDecimal.ZERO);
                _insert.add(CIPayroll.DocumentAbstract.NetTotal, BigDecimal.ZERO);
                _insert.add(CIPayroll.DocumentAbstract.DiscountTotal, 0);
                _insert.add(CIPayroll.DocumentAbstract.RateDiscountTotal, 0);
                _insert.add(CIPayroll.DocumentAbstract.CurrencyId, Currency.getBaseCurrency());
                _insert.add(CIPayroll.DocumentAbstract.RateCurrencyId, Currency.getBaseCurrency());
                _insert.add(CIPayroll.DocumentAbstract.EmployeeAbstractLink,
                                Instance.get(_parameter.getParameterValue(
                                                CIFormPayroll.Payroll_AlterationAbatementForm.employee.name)));
            }
        };
        return create.execute(_parameter);
    }

    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final Edit edit = new Edit()
        {

        };
        return edit.execute(_parameter);
    }

    public Return createDetail(final Parameter _parameter)
        throws EFapsException
    {
        final Create create = new Create()
        {

            @Override
            protected void add2basicInsert(final Parameter _parameter,
                                           final Insert _insert)
                throws EFapsException
            {
                super.add2basicInsert(_parameter, _insert);
                try {
                    final BigDecimal amount = (BigDecimal) NumberFormatter.get().getFormatter().parse(_parameter
                                            .getParameterValue(
                                                CIFormPayroll.Payroll_AlterationAbatementDetailForm.rateAmount.name));

                    _insert.add(CIPayroll.AlterationAbatementDetail.RateAmount, amount);
                    _insert.add(CIPayroll.AlterationAbatementDetail.Amount, amount);
                    _insert.add(CIPayroll.AlterationAbatementDetail.Rate, new Object[] { BigDecimal.ZERO,
                                    BigDecimal.ZERO });
                    _insert.add(CIPayroll.AlterationAbatementDetail.CurrencyLink, Currency.getBaseCurrency());
                    _insert.add(CIPayroll.AlterationAbatementDetail.RateCurrencyLink, Currency.getBaseCurrency());
                } catch (final ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        };
        return create.execute(_parameter);
    }

    public Return editDetail(final Parameter _parameter)
        throws EFapsException
    {
        final Edit edit = new Edit()
        {

            @Override
            protected void add2MainUpdate(final Parameter _parameter,
                                          final Update _update)
                throws EFapsException
            {
                super.add2MainUpdate(_parameter, _update);
                try {
                    final BigDecimal amount = (BigDecimal) NumberFormatter.get().getFormatter().parse(_parameter
                                    .getParameterValue(
                                    CIFormPayroll.Payroll_AlterationAbatementDetailForm.rateAmount.name));
                    _update.add(CIPayroll.AlterationAbatementDetail.RateAmount, amount);
                    _update.add(CIPayroll.AlterationAbatementDetail.Amount, amount);
                } catch (final ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
        return edit.execute(_parameter);
    }

}
