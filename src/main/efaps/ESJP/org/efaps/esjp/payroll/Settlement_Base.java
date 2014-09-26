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

package org.efaps.esjp.payroll;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIFormPayroll;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.sales.document.AbstractSumOnlyDocument;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("89fc5497-2f5f-480e-bb82-cc29259603dd")
@EFapsRevision("$Rev$")
public abstract class Settlement_Base
    extends AbstractSumOnlyDocument
{

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return new return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = createDoc(_parameter);
        connect2Object(_parameter, createdDoc);
        final Return ret = new Return();
        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void add2DocCreate(final Parameter _parameter,
                                 final Insert _insert,
                                 final CreatedDoc _createdDoc)
        throws EFapsException
    {
        super.add2DocCreate(_parameter, _insert, _createdDoc);

        final Instance employeeInst = Instance.get(
                        _parameter.getParameterValue(CIFormPayroll.Payroll_SettlementForm.employee.name));
        _insert.add(CIPayroll.Payslip.EmployeeAbstractLink, employeeInst);
    }

    /**
     * Method to update the fields on leaving the employee field.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return updateFields4Employee(final Parameter _parameter)
        throws EFapsException
    {
        return new Payslip().updateFields4Employee(_parameter);
    }

}
