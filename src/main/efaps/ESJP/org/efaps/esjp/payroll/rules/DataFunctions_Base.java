/*
 * Copyright 2003 - 2014 The eFaps Team
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

package org.efaps.esjp.payroll.rules;

import java.math.BigDecimal;

import org.apache.commons.jexl2.JexlContext;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("891e0b40-53e7-419e-ae42-8942fbb4c0ce")
@EFapsRevision("$Rev$")
public abstract class DataFunctions_Base
{

    /**
     * JexlContext calling the instance.
     */
    private final JexlContext context;

    /**
     * @param _context JexlContext calling the instance.
     */
    public DataFunctions_Base(final JexlContext _context)
    {
        this.context = _context;
    }

    /**
     * Getter method for the instance variable {@link #context}.
     *
     * @return value of instance variable {@link #context}
     */
    public JexlContext getContext()
    {
        return this.context;
    }

    /**
     * @param _keys
     */
    public BigDecimal getAnual(final String... _keys)
        throws EFapsException
    {
        return getAnualMonth(12, _keys);
    }

    /**
     * @param _keys
     */
    public BigDecimal getAnualMonth(final Integer _month,
                                    final String... _keys)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        if (_month > 0 ) {
            final Instance employeeInst = (Instance) getContext().get(AbstractParameter.PARAKEY4EMPLOYINST);
            final DateTime date = (DateTime) getContext().get(AbstractParameter.PARAKEY4DATE);
            final DateTime startDate = date.dayOfYear().withMinimumValue().minusMinutes(1);
            final DateTime endDate = date.monthOfYear().setCopy(_month).dayOfMonth().withMaximumValue().plusMinutes(1);

            final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.Payslip);
            attrQueryBldr.addWhereAttrEqValue(CIPayroll.Payslip.EmployeeAbstractLink, employeeInst);
            attrQueryBldr.addWhereAttrNotEqValue(CIPayroll.Payslip.Status, Status.find(CIPayroll.PayslipStatus.Canceled));
            attrQueryBldr.addWhereAttrGreaterValue(CIPayroll.Payslip.Date, startDate);
            attrQueryBldr.addWhereAttrLessValue(CIPayroll.Payslip.DueDate, endDate);

            final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.PositionAbstract);
            queryBldr.addWhereAttrEqValue(CIPayroll.PositionAbstract.Key, (Object[]) _keys);
            queryBldr.addWhereAttrInQuery(CIPayroll.PositionAbstract.DocumentAbstractLink,
                            attrQueryBldr.getAttributeQuery(CIPayroll.Payslip.ID));
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIPayroll.PositionPayment.Amount);
            multi.execute();
            while (multi.next()) {
                final BigDecimal amount = multi.<BigDecimal>getAttribute(CIPayroll.PositionPayment.Amount);
                ret = ret.add(amount);
            }
        }
        return ret;
    }
}
