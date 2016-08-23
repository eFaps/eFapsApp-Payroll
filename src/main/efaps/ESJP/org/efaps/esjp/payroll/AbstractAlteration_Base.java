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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.jexl2.JexlContext;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIFormPayroll;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.common.uiform.Edit;
import org.efaps.esjp.payroll.rules.AbstractParameter;
import org.efaps.esjp.payroll.rules.AbstractRule_Base;
import org.efaps.esjp.payroll.rules.Calculator;
import org.efaps.esjp.payroll.rules.IDocRuleListener;
import org.efaps.esjp.payroll.rules.IEvaluateListener;
import org.efaps.esjp.payroll.rules.InputRule;
import org.efaps.esjp.sales.document.AbstractDocument;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: $
 */
@EFapsUUID("fdb65459-1f6c-4c55-892f-f71f2e14cf53")
@EFapsApplication("eFapsApp-Payroll")
public abstract class AbstractAlteration_Base
    extends AbstractDocument
{

    /**
     * @param _parameter Parametr as passed by the eFaps API
     * @return Return containing the instance
     * @throws EFapsException on error
     */
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
                AbstractAlteration_Base.this.add2create(_parameter, _insert);
            }
        };
        return create.execute(_parameter);
    }

    /**
     * @param _parameter Parametr as passed by the eFaps API
     * @param _insert       insert to add to
     * @throws EFapsException on error
     */
    protected abstract void add2create(final Parameter _parameter,
                                       final Insert _insert)
        throws EFapsException;

    /**
     * @param _parameter Parametr as passed by the eFaps API
     * @return return containing the instance
     * @throws EFapsException on error
     */
    public Return edit(final Parameter _parameter)
        throws EFapsException
    {
        final Edit edit = new Edit();
        return edit.execute(_parameter);
    }

    /**
     * @param _parameter Parametr as passed by the eFaps API
     * @return return containing the instance
     * @throws EFapsException on error
     */
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
                AbstractAlteration_Base.this.add2createDetail(_parameter, _insert);
            }
        };
        return create.execute(_parameter);
    }

    /**
     * @param _parameter Parametr as passed by the eFaps API
     * @param _insert       insert to add to
     * @throws EFapsException on error
     */
    protected abstract void add2createDetail(final Parameter _parameter,
                                             final Insert _insert)
        throws EFapsException;

    /**
     * @param _parameter Parametr as passed by the eFaps API
     * @return Return containing the instance
     * @throws EFapsException on error
     */
    public Return createDetailMultiple(final Parameter _parameter)
        throws EFapsException
    {

        DateTime date = new DateTime(
                        _parameter.getParameterValue(CIFormPayroll.Payroll_AlterationCreateMultipleForm.date.name));
        final Integer repeats = Integer.parseInt(_parameter
                        .getParameterValue(CIFormPayroll.Payroll_AlterationCreateMultipleForm.repeats.name));

        for (int i = 0; i < repeats; i++) {
            final Parameter parameter = ParameterUtil.clone(_parameter);
            ParameterUtil.setParameterValues(parameter, CIFormPayroll.Payroll_AlterationCreateMultipleForm.date.name,
                            date.toString());
            createDetail(parameter);
            date = date.plusMonths(1);
        }
        return new Return();
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return return containing the instance
     * @throws EFapsException on error
     */
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
                AbstractAlteration_Base.this.add2editDetail(_parameter, _update);
            }
        };
        return edit.execute(_parameter);
    }

    /**
     * @param _parameter Parametr as passed by the eFaps API
     * @param _update       update to add to
     * @throws EFapsException on error
     */
    protected abstract void add2editDetail(final Parameter _parameter,
                                             final Update _update)
        throws EFapsException;


    /**
     * Listener class.
     */
    public static class AlterationListener
        implements IEvaluateListener
    {

        private final AbstractRule_Base<?> rule;

        public AlterationListener(final AbstractRule_Base<?> _abstractRule)
        {
            this.rule = _abstractRule;
        }

        @Override
        public Object onEvaluate(final JexlContext _jexlContext,
                                 final Object _val)
            throws EFapsException
        {
            Object ret = _val;

            final boolean setValue = this.rule instanceof InputRule && (ret == null || ret != null
                            && Calculator.getBigDecimal(ret).compareTo(BigDecimal.ZERO) == 0);

            final DateTime date = (DateTime) _jexlContext.get(AbstractParameter.PARAKEY4DATE);
            final DateTime dueDate = (DateTime) _jexlContext.get(AbstractParameter.PARAKEY4DUEDATE);

            final Instance emplInst = (Instance) _jexlContext.get(AbstractParameter.PARAKEY4EMPLOYINST);
            if (emplInst.isValid()) {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.AlterationAbstract);
                attrQueryBldr.addWhereAttrEqValue(CIPayroll.AlterationAbstract.EmployeeAbstractLink, emplInst);
                attrQueryBldr.addWhereAttrEqValue(CIPayroll.AlterationAbstract.StatusAbstract,
                                Status.find(CIPayroll.AlterationAbatementStatus.Open),
                                Status.find(CIPayroll.AlterationIncrementStatus.Open));

                final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.AlterationDetailAbstract);
                queryBldr.addWhereAttrInQuery(CIPayroll.AlterationDetailAbstract.DocumentAbstractLink,
                                attrQueryBldr.getAttributeQuery(CIPayroll.AlterationAbstract.ID));
                if (dueDate == null) {
                    queryBldr.addWhereAttrIsNull(CIPayroll.AlterationDetailAbstract.ApplyDocumentAbstractLink);
                    queryBldr.addWhereAttrLessValue(CIPayroll.AlterationDetailAbstract.Date, date.plusDays(1));
                } else {
                    queryBldr.addWhereAttrGreaterValue(CIPayroll.AlterationDetailAbstract.Date, date.minusDays(1));
                    queryBldr.addWhereAttrLessValue(CIPayroll.AlterationDetailAbstract.Date, dueDate.plusDays(1));
                }
                queryBldr.addWhereAttrEqValue(CIPayroll.AlterationDetailAbstract.Key, this.rule.getKey());
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CIPayroll.AlterationDetailAbstract.Amount);
                multi.execute();
                final Set<Instance> instances = new HashSet<>();
                while (multi.next()) {
                    if (setValue) {
                        final BigDecimal amount = multi.getAttribute(CIPayroll.AlterationDetailAbstract.Amount);
                        if (ret == null) {
                            ret = amount;
                        } else {
                            ret = Calculator.getBigDecimal(ret).add(amount);
                        }
                    }
                    instances.add(multi.getCurrentInstance());
                }
                if (!instances.isEmpty()) {
                    final ConnectAlterationDetail2Payslip connect = new ConnectAlterationDetail2Payslip(instances);
                    this.rule.addRuleListener(connect);
                }
            }
            return ret;
        }
    }

    /**
     * Connect class.
     */
    public static class ConnectAlterationDetail2Payslip
        implements IDocRuleListener
    {

        /**
         * List of instance to be connected.
         */
        private final Set<Instance> instances;

        /**
         * @param _instances instance
         */
        public ConnectAlterationDetail2Payslip(final Set<Instance> _instances)
        {
            this.instances = _instances;
        }

        /**
         * Getter method for the instance variable {@link #instances}.
         *
         * @return value of instance variable {@link #instances}
         */
        public Set<Instance> getInstances()
        {
            return this.instances;
        }

        @Override
        public void execute(final Parameter _parameter,
                            final Instance _docInst)
            throws EFapsException
        {
            if (_docInst != null && _docInst.isValid() && _docInst.getType().isCIType(CIPayroll.Payslip)) {
                for (final Instance instance : getInstances()) {
                    final Update update = new Update(instance);
                    update.add(CIPayroll.AlterationDetailAbstract.ApplyDocumentAbstractLink, _docInst);
                    update.execute();
                }
            }
        }
    }
}
