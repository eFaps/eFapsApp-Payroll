/*
 * Copyright 2003 - 2013 The eFaps Team
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
import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIFormPayroll;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.payroll.Payslip_Base.Position;
import org.efaps.esjp.payroll.Payslip_Base.SumPosition;
import org.efaps.esjp.payroll.Payslip_Base.TablePos;
import org.efaps.util.EFapsException;


/**
 * Contains method called to calculate one position in a payslip.
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("f1cb9622-af14-4f91-a6e3-ae1832165d84")
@EFapsRevision("$Rev$")
public class PayslipCalculator_Base
{
    /**
     * @param _parameter    Parameter as passed by the eFaps API
     * @param _sums         mapping of instances to sum positions
     * @param _values       mapping of instances to table positions
     * @param _position     current position
     * @return  sum of advances payed
     * @throws EFapsException on error
     */
    public BigDecimal advance(final Parameter _parameter,
                              final Map<Instance, SumPosition> _sums,
                              final Map<Instance, TablePos> _values,
                              final Position _position)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        // get the instance of the employee the payslip belongs to
        final Instance emplInst = Instance.get(_parameter
                        .getParameterValue(CIFormPayroll.Payroll_PayslipForm.number.name));

        if (emplInst.isValid() && emplInst.getType().isKindOf(CIHumanResource.EmployeeAbstract.getType())) {
            final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.Payslip2Advance);
            final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIPayroll.Payslip2Advance.ToLink);

            final QueryBuilder queryBlder = new QueryBuilder(CIPayroll.Advance);
            queryBlder.addWhereAttrEqValue(CIPayroll.Advance.EmployeeAbstractLink, emplInst.getId());
            queryBlder.addWhereAttrNotInQuery(CIPayroll.Advance.ID, attrQuery);
            final MultiPrintQuery multi = queryBlder.getPrint();
            multi.addAttribute(CIPayroll.Advance.Amount2Pay);
            multi.execute();
            while (multi.next()) {
                final BigDecimal pay = multi.<BigDecimal>getAttribute(CIPayroll.Advance.Amount2Pay);
                if (pay != null) {
                    ret = ret.add(pay);
                }
            }
        }
        return ret;
    }
}
