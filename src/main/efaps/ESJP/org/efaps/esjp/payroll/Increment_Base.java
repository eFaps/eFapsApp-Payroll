/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.payroll;

import java.math.BigDecimal;
import java.text.ParseException;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormPayroll;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: $
 */
@EFapsUUID("0be695da-f37e-4b84-89d9-e59fcd326a18")
@EFapsApplication("eFapsApp-Payroll")
public abstract class Increment_Base
    extends AbstractAlteration
{

    @Override
    protected void add2create(final Parameter _parameter,
                              final Insert _insert)
        throws EFapsException
    {
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
        _insert.add(CIPayroll.DocumentAbstract.EmployeeAbstractLink, Instance.get(
                        _parameter.getParameterValue(CIFormPayroll.Payroll_AlterationIncrementForm.employee.name)));
    }

    @Override
    protected void add2createDetail(final Parameter _parameter,
                                    final Insert _insert)
        throws EFapsException
    {
        try {
            final BigDecimal amount = (BigDecimal) NumberFormatter.get().getFormatter().parse(_parameter
                            .getParameterValue(
                            CIFormPayroll.Payroll_AlterationIncrementDetailForm.rateAmount.name));

            _insert.add(CIPayroll.AlterationIncrementDetail.RateAmount, amount);
            _insert.add(CIPayroll.AlterationIncrementDetail.Amount, amount);
            _insert.add(CIPayroll.AlterationIncrementDetail.Rate, new Object[] { BigDecimal.ZERO,
                            BigDecimal.ZERO });
            _insert.add(CIPayroll.AlterationIncrementDetail.CurrencyLink, Currency.getBaseCurrency());
            _insert.add(CIPayroll.AlterationIncrementDetail.RateCurrencyLink, Currency.getBaseCurrency());
        } catch (final ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    protected void add2editDetail(final Parameter _parameter,
                                  final Update _update)
        throws EFapsException
    {
        try {
            final BigDecimal amount = (BigDecimal) NumberFormatter.get().getFormatter().parse(_parameter
                            .getParameterValue(
                            CIFormPayroll.Payroll_AlterationIncrementDetailForm.rateAmount.name));
            _update.add(CIPayroll.AlterationIncrementDetail.RateAmount, amount);
            _update.add(CIPayroll.AlterationIncrementDetail.Amount, amount);
        } catch (final ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
