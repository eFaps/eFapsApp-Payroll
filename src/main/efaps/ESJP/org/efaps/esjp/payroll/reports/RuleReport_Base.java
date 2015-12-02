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
 */

package org.efaps.esjp.payroll.reports;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.efaps.admin.common.MsgPhrase;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.AbstractGroupedByDate;
import org.efaps.esjp.erp.AbstractGroupedByDate_Base.DateGroup;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabMeasureBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabRowGroupBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 *
 * @author The eFaps Team
 */
@EFapsUUID("4c4e6b32-94fa-40aa-9230-a7c6782749d4")
@EFapsApplication("eFapsApp-Payroll")
public abstract class RuleReport_Base
    extends FilteredReport
{
    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing html snipplet
     * @throws EFapsException on error
     */
    public Return generateReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = getReport(_parameter);
        final String html = dyRp.getHtmlSnipplet(_parameter);
        ret.put(ReturnValues.SNIPLETT, html);
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing the file
     * @throws EFapsException on error
     */
    public Return exportReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<?, ?> props = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String mime = (String) props.get("Mime");
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName(getProperty(_parameter, ".FileName"));
        File file = null;
        if ("xls".equalsIgnoreCase(mime)) {
            file = dyRp.getExcel(_parameter);
        } else if ("pdf".equalsIgnoreCase(mime)) {
            file = dyRp.getPDF(_parameter);
        }
        ret.put(ReturnValues.VALUES, file);
        ret.put(ReturnValues.TRUE, true);
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return the report class
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynRuleReport(this);
    }

    /**
     * Dynamic Report.
     */
    public static class DynRuleReport
        extends AbstractDynamicReport
    {

        /** The filtered report. */
        private final RuleReport_Base filteredReport;

        /**
         * Instantiates a new dyn rule report.
         *
         * @param _filteredReport the filtered report
         */
        public DynRuleReport(final RuleReport_Base _filteredReport)
        {
            this.filteredReport = _filteredReport;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            JRRewindableDataSource ret;
            if (getFilteredReport().isCached(_parameter)) {
                ret = getFilteredReport().getDataSourceFromCache(_parameter);
                try {
                    ret.moveFirst();
                } catch (final JRException e) {
                    throw new EFapsException("JRException", e);
                }
            } else {
                final List<DataBean> values = new ArrayList<>();
                final RuleGroupedByDate group = new RuleGroupedByDate();
                final DateTimeFormatter dateTimeFormatter = group.getDateTimeFormatter(DateGroup.MONTH);
                // Payroll_Employee4DocumentMsgPhrase
                final MsgPhrase msgPhrase = MsgPhrase.get(UUID.fromString("4bf03526-3616-4e57-ad70-1e372029ea9e"));

                final QueryBuilder attrQuery = getQueryBldrFromProperties(_parameter);

                final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.PositionAbstract);
                queryBldr.addWhereAttrInQuery(CIPayroll.PositionAbstract.DocumentAbstractLink,
                                attrQuery.getAttributeQuery(CIPayroll.DocumentAbstract.ID));
                add2QueryBuilder(_parameter, queryBldr);
                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selDoc = SelectBuilder.get().linkto(
                                CIPayroll.PositionAbstract.DocumentAbstractLink);
                final SelectBuilder seldDocDate = new SelectBuilder(selDoc).attribute(CIPayroll.DocumentAbstract.Date);
                final SelectBuilder selRule = SelectBuilder.get().linkto(CIPayroll.PositionAbstract.RuleAbstractLink);
                final SelectBuilder selRuleDescr = new SelectBuilder(selRule).attribute(
                                CIPayroll.RuleAbstract.Description);
                final SelectBuilder selRuleKey = new SelectBuilder(selRule).attribute(CIPayroll.RuleAbstract.Key);
                multi.addSelect(seldDocDate, selRuleDescr, selRuleKey);
                multi.addAttribute(CIPayroll.PositionAbstract.Amount);
                multi.addMsgPhrase(selDoc, msgPhrase);
                multi.execute();
                while (multi.next()) {
                    final DateTime date = multi.getSelect(seldDocDate);
                    final String partial = group.getPartial(date, DateGroup.MONTH).toString(dateTimeFormatter);
                    final DataBean bean = new DataBean()
                                    .setPartial(partial)
                                    .setRule(multi.<String>getSelect(selRuleKey) + " - "
                                                    + multi.<String>getSelect(selRuleDescr))
                                    .setAmount(multi.<BigDecimal>getAttribute(CIPayroll.PositionAbstract.Amount))
                                    .setEmployee(multi.getMsgPhrase(selDoc, msgPhrase));
                    values.add(bean);
                }
                ret = new JRBeanCollectionDataSource(values);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        /**
         * @param _parameter Parameter as passed by the eFaps API
         * @param _queryBldr queryBldr to add to
         * @throws EFapsException on error
         */
        protected void add2QueryBuilder(final Parameter _parameter,
                                        final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Map<String, Object> filterMap = getFilteredReport().getFilterMap(_parameter);
            final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.DocumentAbstract);
            if (filterMap.containsKey("dateFrom")) {
                final DateTime date = (DateTime) filterMap.get("dateFrom");
                attrQueryBldr.addWhereAttrGreaterValue(CIPayroll.DocumentAbstract.Date,
                                date.withTimeAtStartOfDay().minusSeconds(1));
            }
            if (filterMap.containsKey("dateTo")) {
                final DateTime date = (DateTime) filterMap.get("dateTo");
                attrQueryBldr.addWhereAttrLessValue(CIPayroll.DocumentAbstract.Date,
                                date.withTimeAtStartOfDay().plusDays(1));
            }

            if (filterMap.containsKey("employee")) {
                final InstanceFilterValue filter = (InstanceFilterValue) filterMap.get("employee");
                if (filter.getObject() != null && filter.getObject().isValid()) {
                    attrQueryBldr.addWhereAttrEqValue(CIPayroll.DocumentAbstract.EmployeeAbstractLink,
                                    filter.getObject());
                }
            }

            if (filterMap.containsKey("rule")) {
                final InstanceSetFilterValue filter = (InstanceSetFilterValue) filterMap.get("rule");
                if (CollectionUtils.isNotEmpty(filter.getObject())) {
                    final Set<String> keys = new HashSet<>();
                    final MultiPrintQuery multi = new MultiPrintQuery(new ArrayList<Instance>(filter.getObject()));
                    multi.addAttribute(CIPayroll.PositionAbstract.Key);
                    multi.execute();
                    while (multi.next()) {
                        keys.add(multi.<String>getAttribute(CIPayroll.PositionAbstract.Key));
                    }
                    _queryBldr.addWhereAttrEqValue(CIPayroll.PositionAbstract.Key, keys.toArray());
                }
            }
            _queryBldr.addWhereAttrInQuery(CIPayroll.PositionAbstract.DocumentAbstractLink,
                            attrQueryBldr.getAttributeQuery(CISales.DocumentAbstract.ID));
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final Map<String, Object> filterMap = getFilteredReport().getFilterMap(_parameter);
            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();
            final CrosstabRowGroupBuilder<String> rowGroup = DynamicReports.ctab.rowGroup("employee", String.class)
                            .setHeaderWidth(150);
            crosstab.addRowGroup(rowGroup);

            final CrosstabRowGroupBuilder<String> ruleGroup = DynamicReports.ctab.rowGroup("rule",
                            String.class);
            crosstab.addRowGroup(ruleGroup);

            if (filterMap.containsKey("employee")) {
                final InstanceFilterValue filter = (InstanceFilterValue) filterMap.get("employee");
                if (filter.getObject() != null && filter.getObject().isValid()) {
                    ruleGroup.setShowTotal(false);
                }
            }
            final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab.columnGroup("partial",
                            String.class);
            crosstab.addColumnGroup(columnGroup);

            final CrosstabMeasureBuilder<BigDecimal> amountMeasure = DynamicReports.ctab.measure(
                            CurrencyInst.get(Currency.getBaseCurrency()).getSymbol(),
                            "amount", BigDecimal.class, Calculation.SUM);
            crosstab.addMeasure(amountMeasure);

            _builder.addSummary(crosstab);
        }

        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        public RuleReport_Base getFilteredReport()
        {
            return this.filteredReport;
        }
    }

    /**
     * The Class DataBean.
     */
    public static class DataBean
    {

        /** The partial. */
        private String partial;

        /** The employee. */
        private String employee;

        /** The rule. */
        private String rule;

        /** The amount. */
        private BigDecimal amount;

        /**
         * Getter method for the instance variable {@link #partial}.
         *
         * @return value of instance variable {@link #partial}
         */
        public String getPartial()
        {
            return this.partial;
        }

        /**
         * Setter method for instance variable {@link #partial}.
         *
         * @param _partial value for instance variable {@link #partial}
         * @return the data bean
         */
        public DataBean setPartial(final String _partial)
        {
            this.partial = _partial;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #employee}.
         *
         * @return value of instance variable {@link #employee}
         */
        public String getEmployee()
        {
            return this.employee;
        }

        /**
         * Setter method for instance variable {@link #employee}.
         *
         * @param _employee value for instance variable {@link #employee}
         * @return the data bean
         */
        public DataBean setEmployee(final String _employee)
        {
            this.employee = _employee;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #rule}.
         *
         * @return value of instance variable {@link #rule}
         */
        public String getRule()
        {
            return this.rule;
        }

        /**
         * Setter method for instance variable {@link #rule}.
         *
         * @param _rule value for instance variable {@link #rule}
         * @return the data bean
         */
        public DataBean setRule(final String _rule)
        {
            this.rule = _rule;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #amount}.
         *
         * @return value of instance variable {@link #amount}
         */
        public BigDecimal getAmount()
        {
            return this.amount;
        }

        /**
         * Setter method for instance variable {@link #amount}.
         *
         * @param _amount value for instance variable {@link #amount}
         * @return the data bean
         */
        public DataBean setAmount(final BigDecimal _amount)
        {
            this.amount = _amount;
            return this;
        }
    }

    /**
     * The Class RuleGroupedByDate.
     */
    public static class RuleGroupedByDate
        extends AbstractGroupedByDate
    {
    }
}
