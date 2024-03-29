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
package org.efaps.esjp.payroll.rules;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.jexl2.JexlContext;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("891e0b40-53e7-419e-ae42-8942fbb4c0ce")
@EFapsApplication("eFapsApp-Payroll")
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
     * @param _keys keys to be analized.
     * @return the amount for anual
     * @throws EFapsException on error
     */
    public BigDecimal getAnual(final String... _keys)
        throws EFapsException
    {
        return getAnualMonth(12, _keys);
    }

    /**
    * @param _month month to be used as filter
    * @param _keys keys to be analized.
    * @return the amount for anual
    * @throws EFapsException on error
    */
    public BigDecimal getAnualMonth(final Integer _month,
                                   final String... _keys)
        throws EFapsException
    {
        return getAnualMonth(_month, true, _keys);
    }

    /**
     * Gets the anual month.
     *
     * @param _month month to be used as filter
     * @param _excludeSettled exclude data that is already settled
     * @param _keys keys to be analized.
     * @return the amount for anual
     * @throws EFapsException on error
     */
    public BigDecimal getAnualMonth(final Integer _month,
                                    final boolean _excludeSettled,
                                    final String... _keys)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        if (_month > 0) {
            final Instance employeeInst = (Instance) getContext().get(AbstractParameter.PARAKEY4EMPLOYINST);
            if (employeeInst != null && employeeInst.isValid()) {
                final DateTime date = (DateTime) getContext().get(AbstractParameter.PARAKEY4DATE);
                final DateTime startDate = date.dayOfYear().withMinimumValue().minusMinutes(1);
                final DateTime endDate = date.monthOfYear().setCopy(_month).dayOfMonth().withMaximumValue()
                                .plusMinutes(1);

                final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.Payslip);
                if (_excludeSettled) {
                    final QueryBuilder relAttrQueryBldr = new QueryBuilder(CIPayroll.Settlement2Payslip);
                    attrQueryBldr.addWhereAttrNotInQuery(CIPayroll.Payslip.ID,
                                relAttrQueryBldr.getAttributeQuery(CIPayroll.Settlement2Payslip.ToLink));
                }

                attrQueryBldr.addWhereAttrEqValue(CIPayroll.Payslip.EmployeeAbstractLink, employeeInst);
                attrQueryBldr.addWhereAttrNotEqValue(CIPayroll.Payslip.Status,
                                Status.find(CIPayroll.PayslipStatus.Canceled));
                attrQueryBldr.addWhereAttrGreaterValue(CIPayroll.Payslip.Date, startDate);
                attrQueryBldr.addWhereAttrLessValue(CIPayroll.Payslip.DueDate, endDate);

                final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.PositionAbstract);
                queryBldr.addWhereAttrEqValue(CIPayroll.PositionAbstract.Key, (Object[]) _keys);
                queryBldr.addWhereAttrInQuery(CIPayroll.PositionAbstract.DocumentAbstractLink,
                                attrQueryBldr.getAttributeQuery(CIPayroll.Payslip.ID));
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selInst = SelectBuilder.get().linkto(
                                CIPayroll.PositionAbstract.DocumentAbstractLink).instance();
                multi.addSelect(selInst);
                multi.addAttribute(CIPayroll.PositionPayment.Amount);
                multi.execute();
                while (multi.next()) {
                    // check if the found is part of the currenct document, if it is exclude it!
                    boolean exclude = false;
                    final Instance currDocInst = (Instance) getContext().get(AbstractParameter.PARAKEY4DOCINST);
                    if (currDocInst != null && currDocInst.isValid()) {
                        final Instance docInst = multi.getSelect(selInst);
                        exclude = currDocInst.equals(docInst);
                    }
                    if (!exclude) {
                        final BigDecimal amount = multi.<BigDecimal>getAttribute(CIPayroll.PositionPayment.Amount);
                        ret = ret.add(amount);
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Get the amount for a list of keys during a period, that have a minimum occurrence.
     * e.g. the amount of extra time only if more than tree times received
     * @param _startDate    start of the period
     * @param _endDate      end of the period
     * @param _minOccurrence minimum occurrence to be evaluated
     * @param _keys keys to be analyzed.
     * @return the amount for the period
     * @throws EFapsException on error
     */
    public BigDecimal getAmount4Period(final DateTime _startDate,
                                       final DateTime _endDate,
                                       final Integer _minOccurrence,
                                       final String... _keys)
        throws EFapsException
    {
        return getAmount4Period(_startDate, _endDate, _minOccurrence, true, _keys);
    }

    /**
     * Get the amount for a list of keys during a period, that have a minimum occurrence.
     * e.g. the amount of extra time only if more than tree times received
     *
     * @param _startDate    start of the period
     * @param _endDate      end of the period
     * @param _minOccurrence minimum occurrence to be evaluated
     * @param _excludeSettled exclude data that is already settled
     * @param _keys keys to be analyzed.
     * @return the amount for the period
     * @throws EFapsException on error
     */
    public BigDecimal getAmount4Period(final DateTime _startDate,
                                       final DateTime _endDate,
                                       final Integer _minOccurrence,
                                       final boolean _excludeSettled,
                                       final String... _keys)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        final Instance employeeInst = (Instance) getContext().get(AbstractParameter.PARAKEY4EMPLOYINST);
        if (employeeInst != null && employeeInst.isValid()) {
            final Set<Integer> months = new HashSet<>();
            final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.Payslip);
            if (_excludeSettled) {
                final QueryBuilder relAttrQueryBldr = new QueryBuilder(CIPayroll.Settlement2Payslip);
                attrQueryBldr.addWhereAttrNotInQuery(CIPayroll.Payslip.ID,
                            relAttrQueryBldr.getAttributeQuery(CIPayroll.Settlement2Payslip.ToLink));
            }

            attrQueryBldr.addWhereAttrEqValue(CIPayroll.Payslip.EmployeeAbstractLink, employeeInst);
            attrQueryBldr.addWhereAttrNotEqValue(CIPayroll.Payslip.Status,
                            Status.find(CIPayroll.PayslipStatus.Canceled));
            attrQueryBldr.addWhereAttrGreaterValue(CIPayroll.Payslip.Date, _startDate.minusMinutes(1));
            attrQueryBldr.addWhereAttrLessValue(CIPayroll.Payslip.DueDate, _endDate.plusMinutes(1));

            final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.PositionAbstract);
            queryBldr.addWhereAttrEqValue(CIPayroll.PositionAbstract.Key, (Object[]) _keys);
            queryBldr.addWhereAttrInQuery(CIPayroll.PositionAbstract.DocumentAbstractLink,
                            attrQueryBldr.getAttributeQuery(CIPayroll.Payslip.ID));
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selDate = SelectBuilder.get().linkto(CIPayroll.PositionAbstract.DocumentAbstractLink)
                            .attribute(CIPayroll.DocumentAbstract.Date);
            multi.addSelect(selDate);
            multi.addAttribute(CIPayroll.PositionPayment.Amount);
            multi.execute();
            while (multi.next()) {
                months.add(multi.<DateTime>getSelect(selDate).getMonthOfYear());
                final BigDecimal amount = multi.<BigDecimal>getAttribute(CIPayroll.PositionPayment.Amount);
                ret = ret.add(amount);
            }
            if (months.size() < _minOccurrence) {
                ret = BigDecimal.ZERO;
            }
        }
        return ret;
    }

    /**
     * Gets the advance.
     *
     * @param _keys the keys
     * @return the amount for anual
     * @throws EFapsException on error
     */
    public BigDecimal getAdvance(final String... _keys)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        final Instance employeeInst = (Instance) getContext().get(AbstractParameter.PARAKEY4EMPLOYINST);
        final Instance docInst = (Instance) getContext().get(AbstractParameter.PARAKEY4DOCINST);
        if (employeeInst != null && employeeInst.isValid()) {
            final Set<Instance> instances = new HashSet<>();
            if (docInst != null && docInst.isValid()) {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.Payslip2Advance);
                attrQueryBldr.addWhereAttrEqValue(CIPayroll.Payslip2Advance.FromLink, docInst);
                final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.Advance);
                queryBldr.addWhereAttrInQuery(CIPayroll.Advance.ID,
                               attrQueryBldr.getAttributeQuery(CIPayroll.Payslip2Advance.ToLink));
                ret = getAdvVal(queryBldr, instances, _keys);
            } else {
                // all advance that are not related yet and belong to the employee
                final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.Payslip2Advance);
                final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.Advance);
                queryBldr.addWhereAttrNotInQuery(CIPayroll.Advance.ID,
                               attrQueryBldr.getAttributeQuery(CIPayroll.Payslip2Advance.ToLink));
                queryBldr.addWhereAttrEqValue(CIPayroll.Advance.EmployeeAbstractLink, employeeInst);
                queryBldr.addWhereAttrEqValue(CIPayroll.Advance.Status, Status.find(CIPayroll.AdvanceStatus.Paid));
                ret = getAdvVal(queryBldr, instances, _keys);

                if (!instances.isEmpty()) {
                    final ConnectAdvance2Payslip connect = new ConnectAdvance2Payslip(instances);
                    final AbstractRule<?> rule = (AbstractRule<?>) getContext().get(Calculator.PARAKEY4CURRENTRULE);
                    if (rule != null) {
                        rule.addRuleListener(connect);
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Gets the adv val.
     *
     * @param _queryBldr the query bldr
     * @param _instances the instances
     * @param _keys the keys
     * @return the adv val
     * @throws EFapsException on error
     */
    protected BigDecimal getAdvVal(final QueryBuilder _queryBldr,
                                   final Set<Instance> _instances,
                                   final String... _keys)
        throws EFapsException
    {
        BigDecimal ret = BigDecimal.ZERO;
        if (_keys == null || _keys != null && _keys.length == 0) {
            final MultiPrintQuery multi = _queryBldr.getPrint();
            multi.addAttribute(CIPayroll.Advance.CrossTotal);
            multi.execute();
            while (multi.next()) {
                ret = ret.add(multi.<BigDecimal>getAttribute(CIPayroll.Advance.CrossTotal));
                _instances.add(multi.getCurrentInstance());
            }
        } else {
            final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.PositionAbstract);
            queryBldr.addWhereAttrInQuery(CIPayroll.PositionAbstract.DocumentAbstractLink,
                            _queryBldr.getAttributeQuery(CIPayroll.Advance.ID));
            queryBldr.addWhereAttrEqValue(CIPayroll.PositionAbstract.Key, (Object[]) _keys);
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIPayroll.PositionAbstract.Amount);
            final SelectBuilder selDocInst = SelectBuilder.get().linkto(CIPayroll.PositionAbstract.DocumentAbstractLink)
                            .instance();
            multi.addSelect(selDocInst);
            multi.execute();
            while (multi.next()) {
                ret = ret.add(multi.<BigDecimal>getAttribute(CIPayroll.PositionAbstract.Amount));
                _instances.add(multi.<Instance>getSelect(selDocInst));
            }
        }
        return ret;
    }


    /**
     * Connect class.
     */
    public static class ConnectAdvance2Payslip
        implements IDocRuleListener
    {

        /**
         * List of instance to be connected.
         */
        private final Set<Instance> instances;

        /**
         * @param _instances instance
         */
        public ConnectAdvance2Payslip(final Set<Instance> _instances)
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
                    final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.Payslip2Advance);
                    queryBldr.addWhereAttrEqValue(CIPayroll.Payslip2Advance.ToLink, instance);
                    if (queryBldr.getQuery().executeWithoutAccessCheck().isEmpty()) {
                        final Insert insert = new Insert(CIPayroll.Payslip2Advance);
                        insert.add(CIPayroll.Payslip2Advance.FromLink, _docInst);
                        insert.add(CIPayroll.Payslip2Advance.ToLink, instance);
                        insert.execute();
                    }
                }
            }
        }
    }
}
