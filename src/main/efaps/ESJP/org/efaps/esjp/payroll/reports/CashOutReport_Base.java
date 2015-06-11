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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
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
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("07b45231-107a-482c-946c-3dda5e6d8262")
@EFapsApplication("eFapsApp-Payroll")
public abstract class CashOutReport_Base
    extends FilteredReport
{

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(CashOutReport.class);

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
        dyRp.setFileName(getDBProperty("FileName"));
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

    @Override
    protected Object getDefaultValue(final Parameter _parameter,
                                     final String _field,
                                     final String _type,
                                     final String _default)
        throws EFapsException
    {
        Object ret;
        if ("Status".equalsIgnoreCase(_type)) {
            final Set<Long> set = new HashSet<>();
            set.add(Status.find(CIPayroll.PayslipStatus.Draft).getId());
            ret = new StatusFilterValue().setObject(set);
        } else {
            ret = super.getDefaultValue(_parameter, _field, _type, _default);
        }
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
        return new DynCashOutReportReport(this);
    }

    /**
     * Dynamic Report.
     */
    public static class DynCashOutReportReport
        extends AbstractDynamicReport
    {

        private FilteredReport filteredReport;

        /**
         * @param _cashOutReport_Base
         */
        public DynCashOutReportReport(final CashOutReport_Base _cashOutReport_Base)
        {
            this.filteredReport = _cashOutReport_Base;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final List<DataBean> values = new ArrayList<>();
            final QueryBuilder queryBldr = getQueryBuilder(_parameter);
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selEmpl = SelectBuilder.get().linkto(CIPayroll.DocumentAbstract.EmployeeAbstractLink);
            final SelectBuilder selEmplNum = new SelectBuilder(selEmpl)
                            .attribute(CIHumanResource.EmployeeAbstract.Number);
            final SelectBuilder selEmplLastName = new SelectBuilder(selEmpl)
                            .attribute(CIHumanResource.EmployeeAbstract.LastName);
            final SelectBuilder selEmplSecondLastName = new SelectBuilder(selEmpl)
                            .attribute(CIHumanResource.EmployeeAbstract.SecondLastName);
            final SelectBuilder selEmplFirstName = new SelectBuilder(selEmpl)
                            .attribute(CIHumanResource.EmployeeAbstract.FirstName);
            final SelectBuilder selCurr = SelectBuilder.get().linkto(CIPayroll.DocumentAbstract.RateCurrencyId)
                            .attribute(CIERP.Currency.Symbol);
            final SelectBuilder selFin = new SelectBuilder(selEmpl).clazz(CIHumanResource.ClassFinancialInformation)
                            .attributeset(CIHumanResource.ClassFinancialInformation.FinancialInformationSet);
            final SelectBuilder selFinTypeInst = new SelectBuilder(selFin)
                            .linkto("FinancialInformationType").instance();
            final SelectBuilder selFinAccount = new SelectBuilder(selFin).attribute("Account");
            final SelectBuilder selFinBankLink = new SelectBuilder(selFin)
                            .linkto("BankLink").attribute(CIContacts.AttributeDefinitionFinancialInstitution.Value);
            multi.addSelect(selEmplNum, selEmplLastName, selEmplSecondLastName, selEmplFirstName, selCurr,
                            selFinTypeInst, selFinAccount, selFinBankLink);
            multi.addAttribute(CIPayroll.DocumentAbstract.RateCrossTotal);
            multi.execute();
            while (multi.next()) {
                final DataBean bean = getBean(_parameter);
                values.add(bean);
                bean.setEmployeeNumber(multi.<String>getSelect(selEmplNum))
                                .setEmployeeLastName(multi.<String>getSelect(selEmplLastName))
                                .setEmployeeSecondLastName(multi.<String>getSelect(selEmplSecondLastName))
                                .setEmployeeFirstName(multi.<String>getSelect(selEmplFirstName))
                                .setTotal(multi.<BigDecimal>getAttribute(CIPayroll.DocumentAbstract.RateCrossTotal))
                                .setCurrency(multi.<String>getSelect(selCurr));
                final Object typeInsts = multi.getSelect(selFinTypeInst);
                final Object accountObj = multi.getSelect(selFinAccount);
                final Object bancObj = multi.getSelect(selFinBankLink);
                if (typeInsts != null) {
                    if (typeInsts instanceof Collection) {
                        final Iterator<?> typeIter = ((Collection<?>) typeInsts).iterator();
                        final Iterator<?> accIter = ((Collection<?>) accountObj).iterator();
                        final Iterator<?> bancIter = ((Collection<?>) bancObj).iterator();
                        while (typeIter.hasNext()) {
                            if (isFinancialInfo(_parameter, (Instance) typeIter.next())) {
                                bean.setAccount((String) accIter.next()).setBanc((String) bancIter.next());
                                break;
                            } else {
                                accIter.next();
                                bancIter.next();
                            }
                        }
                    } else {
                        if (isFinancialInfo(_parameter, (Instance) typeInsts)) {
                            bean.setAccount((String) accountObj).setBanc((String) bancObj);
                        }
                    }
                }
            }
            final ComparatorChain<DataBean> chain = new ComparatorChain<>();
            chain.addComparator(new Comparator<DataBean>()
            {
                @Override
                public int compare(final DataBean _bean0,
                                   final DataBean _bean1)
                {
                    return _bean0.getEmployeeLastName().compareTo(_bean1.getEmployeeLastName());
                }
            });

            chain.addComparator(new Comparator<DataBean>()
            {

                @Override
                public int compare(final DataBean _bean0,
                                   final DataBean _bean1)
                {
                    return _bean0.getEmployeeSecondLastName().compareTo(_bean1.getEmployeeSecondLastName());
                }
            });

            chain.addComparator(new Comparator<DataBean>()
            {

                @Override
                public int compare(final DataBean _bean0,
                                   final DataBean _bean1)
                {
                    return _bean0.getEmployeeFirstName().compareTo(_bean1.getEmployeeFirstName());
                }
            });
            Collections.sort(values, chain);
            return new JRBeanCollectionDataSource(values);
        }

        protected boolean isFinancialInfo(final Parameter _parameter,
                                          final Instance _fintypeInst)
            throws EFapsException
        {
            boolean ret = false;
            final Map<String, Object> filterMap = getFilteredReport().getFilterMap(_parameter);
            if (filterMap.containsKey("finType")) {
                final InstanceFilterValue filter = (InstanceFilterValue) filterMap.get("finType");
                ret = _fintypeInst.equals(filter.getObject());
            }
            return ret;
        }


        protected QueryBuilder getQueryBuilder(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filterMap = getFilteredReport().getFilterMap(_parameter);
            QueryBuilder ret = null;
            boolean added = false;
            if (filterMap.containsKey("type")) {
                final TypeFilterValue filter = (TypeFilterValue) filterMap.get("type");
                if (!filter.getObject().isEmpty()) {
                    for (final Long obj : filter.getObject()) {
                        final Type type = Type.get(obj);
                        if (!added) {
                            added = true;
                            ret = new QueryBuilder(type);
                        } else {
                            ret.addType(type);
                        }
                    }
                }
            }
            if (!added) {
                ret = new QueryBuilder(CIPayroll.DocumentAbstract);
            }

            if (filterMap.containsKey("dateFrom")) {
                final DateTime date = (DateTime) filterMap.get("dateFrom");
                ret.addWhereAttrGreaterValue(CIPayroll.DocumentAbstract.Date,
                                date.withTimeAtStartOfDay().minusSeconds(1));
            }
            if (filterMap.containsKey("dateTo")) {
                final DateTime date = (DateTime) filterMap.get("dateTo");
                ret.addWhereAttrLessValue(CIPayroll.DocumentAbstract.Date,
                                date.withTimeAtStartOfDay().plusDays(1));
            }
            if (filterMap.containsKey("status")) {
                final StatusFilterValue filter = (StatusFilterValue) filterMap.get("status");
                if (!filter.getObject().isEmpty()) {
                    // the documents have the same status keys but must be
                    // selected
                    final Set<Status> status = new HashSet<>();
                    for (final Long obj : filter.getObject()) {
                        final String key = Status.get(obj).getKey();
                        status.add(Status.find(CIPayroll.PayslipStatus, key));
                        status.add(Status.find(CIPayroll.AdvanceStatus, key));
                        status.add(Status.find(CIPayroll.SettlementStatus, key));
                    }
                    ret.addWhereAttrEqValue(CIPayroll.DocumentAbstract.StatusAbstract, status.toArray());
                }
            }
            if (filterMap.containsKey("employee")) {
                final InstanceFilterValue filter = (InstanceFilterValue) filterMap.get("employee");
                if (filter.getObject() != null && filter.getObject().isValid()) {
                    ret.addWhereAttrEqValue(CIPayroll.DocumentAbstract.EmployeeAbstractLink,
                                    filter.getObject());
                }
            }
            return ret;
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final TextColumnBuilder<String> employeeNumberCol = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("EmployeeNumber"), "employeeNumber",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> employeeLastNameCol = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("EmployeeLastName"), "employeeLastName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> employeeSecondLastNameCol = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("EmployeeSecondLastName"), "employeeSecondLastName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> employeeFirstNameCol = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("EmployeeFirstName"), "employeeFirstName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> accountCol = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("Account"), "account", DynamicReports.type.stringType());
            final TextColumnBuilder<String> bancCol = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("Banc"), "banc", DynamicReports.type.stringType());
            final TextColumnBuilder<String> currencyCol = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("Currency"), "currency",
                            DynamicReports.type.stringType()).setWidth(15);
            final TextColumnBuilder<BigDecimal> totalCol = DynamicReports.col.column(
                            getFilteredReport().getDBProperty("Total"), "total", DynamicReports.type.bigDecimalType());
            _builder.addColumn(employeeNumberCol, employeeLastNameCol, employeeSecondLastNameCol, employeeFirstNameCol,
                            bancCol, accountCol, totalCol, currencyCol);
        }

        /**
         * Getter method for the instance variable {@link #filteredReport}.
         *
         * @return value of instance variable {@link #filteredReport}
         */
        public FilteredReport getFilteredReport()
        {
            return this.filteredReport;
        }

        /**
         * Setter method for instance variable {@link #filteredReport}.
         *
         * @param _filteredReport value for instance variable
         *            {@link #filteredReport}
         */
        public void setFilteredReport(final FilteredReport _filteredReport)
        {
            this.filteredReport = _filteredReport;
        }

        protected DataBean getBean(final Parameter _parameter)
        {
            return new DataBean();
        }

    }

    public static class DataBean
    {

        private BigDecimal total;
        private String currency;
        private String employeeNumber;
        private String employeeLastName;
        private String employeeSecondLastName;
        private String employeeFirstName;
        private String account;
        private String banc;

        /**
         * Getter method for the instance variable {@link #employeeNumber}.
         *
         * @return value of instance variable {@link #employeeNumber}
         */
        public String getEmployeeNumber()
        {
            return this.employeeNumber;
        }

        /**
         * Setter method for instance variable {@link #employeeNumber}.
         *
         * @param _employeeNumber value for instance variable
         *            {@link #employeeNumber}
         */
        public DataBean setEmployeeNumber(final String _employeeNumber)
        {
            this.employeeNumber = _employeeNumber;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #employeeLastName}.
         *
         * @return value of instance variable {@link #employeeLastName}
         */
        public String getEmployeeLastName()
        {
            return this.employeeLastName;
        }

        /**
         * Setter method for instance variable {@link #employeeLastName}.
         *
         * @param _employeeLastName value for instance variable
         *            {@link #employeeLastName}
         */
        public DataBean setEmployeeLastName(final String _employeeLastName)
        {
            this.employeeLastName = _employeeLastName;
            return this;
        }

        /**
         * Getter method for the instance variable
         * {@link #employeeSecondLastName}.
         *
         * @return value of instance variable {@link #employeeSecondLastName}
         */
        public String getEmployeeSecondLastName()
        {
            return this.employeeSecondLastName;
        }

        /**
         * Setter method for instance variable {@link #employeeSecondLastName}.
         *
         * @param _employeeSecondLastName value for instance variable
         *            {@link #employeeSecondLastName}
         */
        public DataBean setEmployeeSecondLastName(final String _employeeSecondLastName)
        {
            this.employeeSecondLastName = _employeeSecondLastName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #employeeFirstName}.
         *
         * @return value of instance variable {@link #employeeFirstName}
         */
        public String getEmployeeFirstName()
        {
            return this.employeeFirstName;
        }

        /**
         * Setter method for instance variable {@link #employeeFirstName}.
         *
         * @param _employeeFirstName value for instance variable
         *            {@link #employeeFirstName}
         */
        public DataBean setEmployeeFirstName(final String _employeeFirstName)
        {
            this.employeeFirstName = _employeeFirstName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #total}.
         *
         * @return value of instance variable {@link #total}
         */
        public BigDecimal getTotal()
        {
            return this.total;
        }

        /**
         * Setter method for instance variable {@link #total}.
         *
         * @param _total value for instance variable {@link #total}
         */
        public DataBean setTotal(final BigDecimal _total)
        {
            this.total = _total;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #currency}.
         *
         * @return value of instance variable {@link #currency}
         */
        public String getCurrency()
        {
            return this.currency;
        }

        /**
         * Setter method for instance variable {@link #currency}.
         *
         * @param _currency value for instance variable {@link #currency}
         */
        public DataBean setCurrency(final String _currency)
        {
            this.currency = _currency;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #account}.
         *
         * @return value of instance variable {@link #account}
         */
        public String getAccount()
        {
            return this.account;
        }

        /**
         * Setter method for instance variable {@link #account}.
         *
         * @param _account value for instance variable {@link #account}
         */
        public DataBean setAccount(final String _account)
        {
            this.account = _account;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #banc}.
         *
         * @return value of instance variable {@link #banc}
         */
        public String getBanc()
        {
            return this.banc;
        }

        /**
         * Setter method for instance variable {@link #banc}.
         *
         * @param _banc value for instance variable {@link #banc}
         */
        public DataBean setBanc(final String _banc)
        {
            this.banc = _banc;
            return this;
        }
    }
}
