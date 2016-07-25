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

package org.efaps.esjp.payroll.listener;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.projects.listener.IOnResultReport;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: $
 */
@EFapsUUID("2e8cba43-cd3a-4738-932d-59ba42186b4b")
@EFapsApplication("eFapsApp-Payroll")
public abstract class OnProjectResultReport_Base
    implements IOnResultReport
{

    @Override
    public boolean addAmounts2DocBean(final Parameter _parameter,
                                      final Instance _instance,
                                      final BigDecimal _net,
                                      final BigDecimal _cross,
                                      final Object _bean,
                                      final boolean _add)
        throws EFapsException
    {
        boolean ret = true;
        if (_instance.getType().isCIType(CIPayroll.Payslip)) {
            ret = false;
            final PrintQuery print = new PrintQuery(_instance);
            print.addAttribute(CIPayroll.Payslip.AmountCost);
            print.execute();
            final BigDecimal cost = print.getAttribute(CIPayroll.Payslip.AmountCost);
            try {
                _bean.getClass().getMethod("addNet", BigDecimal.class).invoke(_bean, cost);
                _bean.getClass().getMethod("addCross", BigDecimal.class).invoke(_bean, cost);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
                            | NoSuchMethodException | SecurityException e) {
                throw new EFapsException("Catched error", e);
            }

        }
        return ret;
    }

    @Override
    public int getWeight()
    {
        return 0;
    }
}
