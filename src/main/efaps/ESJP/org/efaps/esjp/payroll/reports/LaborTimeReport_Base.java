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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnTitleGroupBuilder;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.dbproperty.DBProperties;
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
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("925d60d4-b87c-4ed7-a224-f69e820511b2")
@EFapsApplication("eFapsApp-Payroll")
public abstract class LaborTimeReport_Base
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
        dyRp.setFileName(DBProperties.getProperty(LaborTimeReport.class.getName() + ".FileName"));
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
        return new DynLaborTimeReport(this);
    }

    /**
     * Dynamic Report.
     */
    public static class DynLaborTimeReport
        extends AbstractDynamicReport
    {

        private final LaborTimeReport_Base filterReport;

        /**
         * @param _ruleReport_Base
         */
        public DynLaborTimeReport(final LaborTimeReport_Base _ruleReport_Base)
        {
            this.filterReport = _ruleReport_Base;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final Map<Instance, LaborTimeBean> values = new HashMap<>();
            final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.Payslip);
            add2QueryBuilder(_parameter, queryBldr);
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selEmpl = SelectBuilder.get().linkto(CIPayroll.Payslip.EmployeeAbstractLink);
            final SelectBuilder selEmplInst = new SelectBuilder(selEmpl).instance();
            final SelectBuilder selEmplNumber = new SelectBuilder(selEmpl)
                            .attribute(CIHumanResource.EmployeeAbstract.Number);
            final SelectBuilder selEmplFirstName = new SelectBuilder(selEmpl)
                            .attribute(CIHumanResource.EmployeeAbstract.FirstName);
            final SelectBuilder selEmplLastName = new SelectBuilder(selEmpl)
                            .attribute(CIHumanResource.EmployeeAbstract.LastName);
            final SelectBuilder selEmplSecondLastName = new SelectBuilder(selEmpl)
                            .attribute(CIHumanResource.EmployeeAbstract.SecondLastName);
            multi.addSelect(selEmplInst, selEmplNumber, selEmplFirstName, selEmplLastName, selEmplSecondLastName);
            multi.addAttribute(CIPayroll.Payslip.LaborTime, CIPayroll.Payslip.ExtraLaborTime,
                            CIPayroll.Payslip.NightLaborTime, CIPayroll.Payslip.HolidayLaborTime);
            multi.execute();
            while (multi.next()) {
                final Instance emplInst = multi.getSelect(selEmplInst);
                LaborTimeBean bean;
                if (values.containsKey(emplInst)) {
                    bean = values.get(emplInst);
                } else {
                    bean = getBean();
                    values.put(emplInst, bean);
                    bean.setNumber(multi.<String>getSelect(selEmplNumber))
                                    .setFirstName(multi.<String>getSelect(selEmplFirstName))
                                    .setLastName(multi.<String>getSelect(selEmplLastName))
                                    .setSecondLastName(multi.<String>getSelect(selEmplSecondLastName));
                }
                bean.addLaborTime(multi.getAttribute(CIPayroll.Payslip.LaborTime))
                                .addExtraLaborTime(multi.getAttribute(CIPayroll.Payslip.ExtraLaborTime))
                                .addNightLaborTime(multi.getAttribute(CIPayroll.Payslip.NightLaborTime))
                                .addHolidayLaborTime(multi.getAttribute(CIPayroll.Payslip.HolidayLaborTime));
            }
            final ComparatorChain<LaborTimeBean> chain = new ComparatorChain<>();
            chain.addComparator(new Comparator<LaborTimeBean>()
            {
                @Override
                public int compare(final LaborTimeBean _bean0,
                                   final LaborTimeBean _bean1)
                {
                    return _bean0.getLastName().compareTo(_bean1.getLastName());
                }
            });

            chain.addComparator(new Comparator<LaborTimeBean>()
            {

                @Override
                public int compare(final LaborTimeBean _bean0,
                                   final LaborTimeBean _bean1)
                {
                    return _bean0.getSecondLastName().compareTo(_bean1.getSecondLastName());
                }
            });

            chain.addComparator(new Comparator<LaborTimeBean>()
            {

                @Override
                public int compare(final LaborTimeBean _bean0,
                                   final LaborTimeBean _bean1)
                {
                    return _bean0.getFirstName().compareTo(_bean1.getFirstName());
                }
            });

            final List<LaborTimeBean> temp = new ArrayList<>(values.values());
            Collections.sort(temp, chain);
            return new JRBeanCollectionDataSource(temp);
        }

        protected void add2QueryBuilder(final Parameter _parameter,
                                        final QueryBuilder _queryBldr)
            throws EFapsException
        {
            _queryBldr.addWhereAttrNotEqValue(CIPayroll.DocumentAbstract.StatusAbstract,
                            Status.find(CIPayroll.PayslipStatus.Canceled));
            final Map<String, Object> filterMap = getFilterReport().getFilterMap(_parameter);
            if (filterMap.containsKey("dateFrom")) {
                final DateTime date = (DateTime) filterMap.get("dateFrom");
                _queryBldr.addWhereAttrGreaterValue(CIPayroll.DocumentAbstract.Date,
                                date.withTimeAtStartOfDay().minusSeconds(1));
            }
            if (filterMap.containsKey("dateTo")) {
                final DateTime date = (DateTime) filterMap.get("dateTo");
                _queryBldr.addWhereAttrLessValue(CIPayroll.DocumentAbstract.Date,
                                date.withTimeAtStartOfDay().plusDays(1));
            }
            if (filterMap.containsKey("employee")) {
                final InstanceFilterValue filter = (InstanceFilterValue) filterMap.get("employee");
                if (filter.getObject() != null && filter.getObject().isValid()) {
                    _queryBldr.addWhereAttrEqValue(CIPayroll.DocumentAbstract.EmployeeAbstractLink,
                                    filter.getObject());
                }
            }
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {

            final TextColumnBuilder<String> numberColumn = DynamicReports.col.column(
                            getLabel("Number"),
                            "number", DynamicReports.type.stringType());
            final TextColumnBuilder<String> firstNameColumn = DynamicReports.col.column(
                            getLabel("FirstName"),
                            "firstName", DynamicReports.type.stringType());
            final TextColumnBuilder<String> lastNameColumn = DynamicReports.col.column(
                            getLabel("LastName"),
                            "lastName", DynamicReports.type.stringType());
            final TextColumnBuilder<String> secondLastNameColumn = DynamicReports.col.column(
                            getLabel("SecondLastName"),
                            "secondLastName", DynamicReports.type.stringType());

            final TextColumnBuilder<BigDecimal> laborTimeDaysColumn = DynamicReports.col.column(
                            getLabel("LaborTimeDays"),
                            "laborTimeDays", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> laborTimeHoursColumn = DynamicReports.col.column(
                            getLabel("LaborTimeHours"),
                            "laborTimeHours", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> extraLaborTimeDaysColumn = DynamicReports.col.column(
                            getLabel("ExtraLaborTimeDays"),
                            "extraLaborTimeDays", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> extraLaborTimeHoursColumn = DynamicReports.col.column(
                            getLabel("ExtraLaborTimeHours"),
                            "extraLaborTimeHours", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> nightLaborTimeDaysColumn = DynamicReports.col.column(
                            getLabel("NightLaborTimeDays"),
                            "nightLaborTimeDays", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> nightLaborTimeHoursColumn = DynamicReports.col.column(
                            getLabel("NightLaborTimeHours"),
                            "nightLaborTimeHours", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> holidayLaborTimeDaysColumn = DynamicReports.col.column(
                            getLabel("HolidayLaborTimeDays"),
                            "holidayLaborTimeDays", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> holidayLaborTimeHoursColumn = DynamicReports.col.column(
                            getLabel("HolidayLaborTimeHours"),
                            "holidayLaborTimeHours", DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> totalTimeDaysColumn = DynamicReports.col.column(
                            getLabel("TotalTimeDays"),
                            "totalTimeDays", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> totalTimeHoursColumn = DynamicReports.col.column(
                            getLabel("TotalTimeHours"),
                            "totalTimeHours", DynamicReports.type.bigDecimalType());

            final ColumnTitleGroupBuilder employeeGroup = DynamicReports.grid.titleGroup(getLabel("EmployeeGroup"),
                            numberColumn, firstNameColumn, lastNameColumn, secondLastNameColumn);

            final ColumnTitleGroupBuilder laborTimeGroup = DynamicReports.grid.titleGroup(getLabel("LaborTimeGroup"),
                            laborTimeDaysColumn, laborTimeHoursColumn);
            final ColumnTitleGroupBuilder extralaborTimeGroup = DynamicReports.grid.titleGroup(
                            getLabel("ExtraLaborTimeGroup"), extraLaborTimeDaysColumn, extraLaborTimeHoursColumn);
            final ColumnTitleGroupBuilder nightlaborTimeGroup = DynamicReports.grid.titleGroup(
                            getLabel("NightLaborTimeGroup"), nightLaborTimeDaysColumn, nightLaborTimeHoursColumn);
            final ColumnTitleGroupBuilder holidaylaborTimeGroup = DynamicReports.grid.titleGroup(
                            getLabel("HolidayLaborTimeGroup"), holidayLaborTimeDaysColumn, holidayLaborTimeHoursColumn);
            final ColumnTitleGroupBuilder totalTimeGroup = DynamicReports.grid.titleGroup(
                            getLabel("TotalTimeGroup"), totalTimeDaysColumn, totalTimeHoursColumn);
            _builder.columnGrid(employeeGroup, laborTimeGroup, extralaborTimeGroup, nightlaborTimeGroup,
                            holidaylaborTimeGroup, totalTimeGroup)
                            .addColumn(numberColumn, firstNameColumn, lastNameColumn, secondLastNameColumn,
                                            laborTimeDaysColumn, laborTimeHoursColumn, extraLaborTimeDaysColumn,
                                            extraLaborTimeHoursColumn, nightLaborTimeDaysColumn,
                                            nightLaborTimeHoursColumn, holidayLaborTimeDaysColumn,
                                            holidayLaborTimeHoursColumn, totalTimeDaysColumn, totalTimeHoursColumn);
        }

        /**
         * @param _key key the label is wanted for
         * @return label
         */
        protected String getLabel(final String _key)
        {
            return DBProperties.getProperty(LaborTimeReport.class.getName() + "." + _key);
        }

        /**
         * Getter method for the instance variable {@link #filterReport}.
         *
         * @return value of instance variable {@link #filterReport}
         */
        public LaborTimeReport_Base getFilterReport()
        {
            return this.filterReport;
        }

        protected LaborTimeBean getBean()
        {
            return new LaborTimeBean();
        }
    }

    public static class LaborTimeBean
    {

        private static BigDecimal DAY = BigDecimal.valueOf(8);
        private BigDecimal laborTime = BigDecimal.ZERO;
        private BigDecimal extraLaborTime = BigDecimal.ZERO;
        private BigDecimal nightLaborTime = BigDecimal.ZERO;
        private BigDecimal holidayLaborTime = BigDecimal.ZERO;
        private String number;

        private String firstName;
        private String lastName;
        private String secondLastName;

        public BigDecimal getLaborTimeDays()
        {
            BigDecimal ret = BigDecimal.ZERO;
            if (this.laborTime.compareTo(BigDecimal.ZERO) != 0) {
                ret = this.laborTime.divideToIntegralValue(DAY);
            }
            return ret;
        }

        public BigDecimal getLaborTimeHours()
        {
            BigDecimal ret = BigDecimal.ZERO;
            if (this.laborTime.compareTo(BigDecimal.ZERO) != 0) {
                ret = this.laborTime.remainder(DAY);
            }
            return ret;
        }

        public BigDecimal getExtraLaborTimeDays()
        {
            BigDecimal ret = BigDecimal.ZERO;
            if (this.extraLaborTime.compareTo(BigDecimal.ZERO) != 0) {
                ret = this.extraLaborTime.divideToIntegralValue(DAY);
            }
            return ret;
        }

        public BigDecimal getExtraLaborTimeHours()
        {
            BigDecimal ret = BigDecimal.ZERO;
            if (this.extraLaborTime.compareTo(BigDecimal.ZERO) != 0) {
                ret = this.extraLaborTime.remainder(DAY);
            }
            return ret;
        }

        public BigDecimal getNightLaborTimeDays()
        {
            BigDecimal ret = BigDecimal.ZERO;
            if (this.nightLaborTime.compareTo(BigDecimal.ZERO) != 0) {
                ret = this.nightLaborTime.divideToIntegralValue(DAY);
            }
            return ret;
        }

        public BigDecimal getNightLaborTimeHours()
        {
            BigDecimal ret = BigDecimal.ZERO;
            if (this.nightLaborTime.compareTo(BigDecimal.ZERO) != 0) {
                ret = this.nightLaborTime.remainder(DAY);
            }
            return ret;
        }

        public BigDecimal getHolidayLaborTimeDays()
        {
            BigDecimal ret = BigDecimal.ZERO;
            if (this.holidayLaborTime.compareTo(BigDecimal.ZERO) != 0) {
                ret = this.holidayLaborTime.divideToIntegralValue(DAY);
            }
            return ret;
        }

        public BigDecimal getHolidayLaborTimeHours()
        {
            BigDecimal ret = BigDecimal.ZERO;
            if (this.holidayLaborTime.compareTo(BigDecimal.ZERO) != 0) {
                ret = this.holidayLaborTime.remainder(DAY);
            }
            return ret;
        }

        public BigDecimal getTotalTimeDays()
        {
            BigDecimal ret = this.laborTime.add(this.extraLaborTime).add(this.nightLaborTime)
                            .add(this.holidayLaborTime);
            if (ret.compareTo(BigDecimal.ZERO) != 0) {
                ret = ret.divideToIntegralValue(DAY);
            }
            return ret;
        }

        public BigDecimal getTotalTimeHours()
        {
            BigDecimal ret = this.laborTime.add(this.extraLaborTime).add(this.nightLaborTime)
                            .add(this.holidayLaborTime);
            if (ret.compareTo(BigDecimal.ZERO) != 0) {
                ret = ret.remainder(DAY);
            }
            return ret;
        }

        /**
         * @param _attribute
         */
        public LaborTimeBean addLaborTime(final Object _attribute)
        {
            if (_attribute != null) {
                this.laborTime = this.laborTime.add(evalTime(_attribute));
            }
            return this;
        }

        /**
         * @param _attribute
         */
        public LaborTimeBean addExtraLaborTime(final Object _attribute)
        {
            if (_attribute != null) {
                this.extraLaborTime = this.extraLaborTime.add(evalTime(_attribute));
            }
            return this;
        }

        /**
         * @param _attribute
         */
        public LaborTimeBean addNightLaborTime(final Object _attribute)
        {
            if (_attribute != null) {
                this.nightLaborTime = this.nightLaborTime.add(evalTime(_attribute));
            }
            return this;
        }

        /**
         * @param _attribute
         */
        public LaborTimeBean addHolidayLaborTime(final Object _attribute)
        {
            if (_attribute != null) {
                this.holidayLaborTime = this.holidayLaborTime.add(evalTime(_attribute));
            }
            return this;
        }

        protected BigDecimal evalTime(final Object _attribute)
        {
            BigDecimal ret = BigDecimal.ZERO;
            if (_attribute != null) {
                BigDecimal amount = (BigDecimal) ((Object[]) _attribute)[0];
                final UoM uoM = (UoM) ((Object[]) _attribute)[1];
                if (amount == null) {
                    amount = BigDecimal.ZERO;
                }
                if (uoM.getDimension().getBaseUoM().equals(uoM)) {
                    ret = amount;
                } else if (amount.compareTo(BigDecimal.ZERO) != 0) {
                    ret = amount.multiply(BigDecimal.valueOf(uoM.getNumerator()))
                                    .setScale(8, BigDecimal.ROUND_HALF_UP)
                                    .divide(BigDecimal.valueOf(uoM.getDenominator()), BigDecimal.ROUND_HALF_UP);
                }
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #number}.
         *
         * @return value of instance variable {@link #number}
         */
        public String getNumber()
        {
            return this.number;
        }

        /**
         * Setter method for instance variable {@link #number}.
         *
         * @param _number value for instance variable {@link #number}
         */
        public LaborTimeBean setNumber(final String _number)
        {
            this.number = _number;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #firstName}.
         *
         * @return value of instance variable {@link #firstName}
         */
        public String getFirstName()
        {
            return this.firstName;
        }

        /**
         * Setter method for instance variable {@link #firstName}.
         *
         * @param _firstName value for instance variable {@link #firstName}
         */
        public LaborTimeBean setFirstName(final String _firstName)
        {
            this.firstName = _firstName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #lastName}.
         *
         * @return value of instance variable {@link #lastName}
         */
        public String getLastName()
        {
            return this.lastName == null ? "" : this.lastName;
        }

        /**
         * Setter method for instance variable {@link #lastName}.
         *
         * @param _lastName value for instance variable {@link #lastName}
         */
        public LaborTimeBean setLastName(final String _lastName)
        {
            this.lastName = _lastName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #secondLastName}.
         *
         * @return value of instance variable {@link #secondLastName}
         */
        public String getSecondLastName()
        {
            return this.secondLastName == null ? "" : this.secondLastName;
        }

        /**
         * Setter method for instance variable {@link #secondLastName}.
         *
         * @param _secondLastName value for instance variable
         *            {@link #secondLastName}
         */
        public LaborTimeBean setSecondLastName(final String _secondLastName)
        {
            this.secondLastName = _secondLastName;
            return this;
        }
    }
}
