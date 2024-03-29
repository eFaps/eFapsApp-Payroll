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

import java.io.File;
import java.util.List;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIFormPayroll;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.payroll.basis.BasisAttribute;
import org.efaps.esjp.payroll.rules.AbstractRule;
import org.efaps.esjp.payroll.rules.Calculator;
import org.efaps.esjp.payroll.rules.Result;
import org.efaps.esjp.sales.document.AbstractDocumentSum;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("89fc5497-2f5f-480e-bb82-cc29259603dd")
@EFapsApplication("eFapsApp-Payroll")
public abstract class Settlement_Base
    extends AbstractDocumentSum
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
        connect2Payslip(_parameter, createdDoc);

        final Return ret = new Return();

        final Payslip payslip = new Payslip();

        final Instance rateCurrInst = getRateCurrencyInstance(_parameter, createdDoc);

        final Object[] rateObj = getRateObject(_parameter);

        final List<? extends AbstractRule<?>> rules = payslip.analyseRulesFomUI(_parameter,
                        payslip.getRuleInstFromUI(_parameter));

        final Result result = Calculator.getResult(_parameter, rules);
        payslip.updateTotals(_parameter, createdDoc.getInstance(), result, rateCurrInst, rateObj);
        payslip.updatePositions(_parameter, createdDoc.getInstance(), result, rateCurrInst, rateObj);

        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }

        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return ret;
    }

    /**
     * Connect2 payslip.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @param _createdDoc the created doc
     * @throws EFapsException on error
     */
    protected void connect2Payslip(final Parameter _parameter,
                                   final CreatedDoc _createdDoc)
        throws EFapsException
    {
        final PrintQuery print = new PrintQuery(_createdDoc.getInstance());
        print.addAttribute(CIPayroll.DocumentAbstract.EmployeeAbstractLink, CIPayroll.Settlement.EndDate);
        print.execute();

        final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.Settlement2Payslip);

        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.Payslip);
        queryBldr.addWhereAttrNotInQuery(CIPayroll.Payslip.ID,
                        attrQueryBldr.getAttributeQuery(CIPayroll.Settlement2Payslip.ToLink));
        queryBldr.addWhereAttrEqValue(CIPayroll.Payslip.EmployeeAbstractLink,
                        print.<Long>getAttribute(CIPayroll.DocumentAbstract.EmployeeAbstractLink));
        queryBldr.addWhereAttrLessValue(CIPayroll.Payslip.Date, print.getAttribute(CIPayroll.Settlement.EndDate));
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        while (query.next()) {
            final Insert insert = new Insert(CIPayroll.Settlement2Payslip);
            insert.add(CIPayroll.Settlement2Payslip.FromAbstractLink, _createdDoc.getInstance());
            insert.add(CIPayroll.Settlement2Payslip.ToAbstractLink, query.getCurrentValue());
            insert.execute();
        }

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
        _insert.add(CIPayroll.Settlement.EmployeeAbstractLink, employeeInst);
        final Instance templateInst = Instance.get(
                        _parameter.getParameterValue(CIFormPayroll.Payroll_SettlementForm.template.name));
        _insert.add(CIPayroll.Settlement.TemplateLinkAbstract, templateInst);
        _insert.add(CIPayroll.Settlement.StartDate,
                        _parameter.getParameterValue(CIFormPayroll.Payroll_SettlementForm.startDate.name));
        _insert.add(CIPayroll.Settlement.EndDate,
                        _parameter.getParameterValue(CIFormPayroll.Payroll_SettlementForm.endDate.name));
        _insert.add(CIPayroll.Settlement.Basis, BasisAttribute.getValueList4Inst(_parameter, employeeInst));
    }

    /**
     * Edits the.
     *
     * @param _parameter Parameter as passed by the eFaps API
     * @return the return
     * @throws EFapsException on error
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final CreatedDoc createdDoc = editDoc(_parameter);
        connect2Object(_parameter, createdDoc);
        final Return ret = new Return();

        final Payslip payslip = new Payslip();

        final Instance rateCurrInst = getRateCurrencyInstance(_parameter, createdDoc);

        final Object[] rateObj = getRateObject(_parameter);

        final List<? extends AbstractRule<?>> rules = payslip.analyseRulesFomUI(_parameter,
                        payslip.getRuleInstFromUI(_parameter));

        final Result result = Calculator.getResult(_parameter, rules);
        payslip.updateTotals(_parameter, createdDoc.getInstance(), result, rateCurrInst, rateObj);
        payslip.updatePositions(_parameter, createdDoc.getInstance(), result, rateCurrInst, rateObj);

        final File file = createReport(_parameter, createdDoc);
        if (file != null) {
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        }

        ret.put(ReturnValues.INSTANCE, createdDoc.getInstance());
        return ret;
    }

    @Override
    protected void add2DocEdit(final Parameter _parameter,
                               final Update _update,
                               final EditedDoc _editDoc)
        throws EFapsException
    {
        super.add2DocEdit(_parameter, _update, _editDoc);
        _update.add(CIPayroll.Settlement.StartDate,
                        _parameter.getParameterValue(CIFormPayroll.Payroll_SettlementForm.startDate.name));
        _update.add(CIPayroll.Settlement.EndDate,
                        _parameter.getParameterValue(CIFormPayroll.Payroll_SettlementForm.endDate.name));
        _update.add(CIPayroll.Settlement.Basis, BasisAttribute.getValueList4Inst(_parameter, _update.getInstance()));
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
