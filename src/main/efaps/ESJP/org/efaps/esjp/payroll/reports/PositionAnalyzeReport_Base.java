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

package org.efaps.esjp.payroll.reports;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabMeasureBuilder;
import net.sf.dynamicreports.report.builder.crosstab.CrosstabRowGroupBuilder;
import net.sf.dynamicreports.report.constant.Calculation;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.apache.commons.lang.BooleanUtils;
import org.efaps.admin.common.MsgPhrase;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.sales.report.DocumentSumReport;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: PositionAnalyzeReport_Base.java 14584 2014-12-04 03:55:22Z
 *          jan@moxter.net $
 */
public abstract class PositionAnalyzeReport_Base
    extends FilteredReport
{

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(PositionAnalyzeReport.class);

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
        dyRp.setFileName(DBProperties.getProperty(DocumentSumReport.class.getName() + ".FileName"));
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
        return new DynPositionAnalyzeReport(this);
    }

    /**
     * Dynamic Report.
     */
    public static class DynPositionAnalyzeReport
        extends AbstractDynamicReport
    {

        private final PositionAnalyzeReport_Base filterReport;

        /**
         * @param _filterReport report
         */
        public DynPositionAnalyzeReport(final PositionAnalyzeReport_Base _filterReport)
        {
            this.filterReport = _filterReport;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final Collection<DataBean> datasource = new ArrayList<>();
            final Map<String, Object> filterMap = getFilterReport().getFilterMap(_parameter);
            Boolean switchVal = false;
            if (filterMap.containsKey("switch")) {
                switchVal = (Boolean) filterMap.get("switch");
            }
            final QueryBuilder queryBuilder = new QueryBuilder(CIPayroll.PositionDeduction);
            queryBuilder.addType(CIPayroll.PositionNeutral, CIPayroll.PositionPayment);
            add2QueryBuilder(_parameter, queryBuilder);
            final MultiPrintQuery multi = queryBuilder.getPrint();
            final SelectBuilder selDoc = SelectBuilder.get().linkto(CIPayroll.PositionAbstract.DocumentAbstractLink);
            final SelectBuilder selDocInst = new SelectBuilder(selDoc).instance();
            final SelectBuilder selDocName = new SelectBuilder(selDoc).attribute(CIPayroll.DocumentAbstract.Name);
            final SelectBuilder selDepName = new SelectBuilder(selDoc).linkto(CIPayroll.DocumentAbstract.EmployeeAbstractLink)
                .linkfrom(CIHumanResource.Department2EmployeeAdminister.EmployeeLink)
                .linkto(CIHumanResource.Department2EmployeeAdminister.DepartmentLink)
                .attribute(CIHumanResource.Department.Name);
            final MsgPhrase msgPhrase = MsgPhrase.get(UUID.fromString("4bf03526-3616-4e57-ad70-1e372029ea9e"));
            multi.addMsgPhrase(selDoc, msgPhrase);
            multi.addSelect(selDocInst, selDocName, selDepName);
            multi.addAttribute(CIPayroll.PositionAbstract.Amount, CIPayroll.PositionAbstract.Description,
                            CIPayroll.PositionAbstract.Key);
            multi.execute();
            while (multi.next()) {
                final Instance docInst = multi.getSelect(selDocInst);
                final DataBean bean = new DataBean().setDocInst(docInst)
                                .setSwitched(switchVal)
                                .setDocName(multi.<String>getSelect(selDocName))
                                .setPosInst(multi.getCurrentInstance())
                                .setPosDescr(multi.<String>getAttribute(CIPayroll.PositionAbstract.Description))
                                .setPosKey(multi.<String>getAttribute(CIPayroll.PositionAbstract.Key))
                                .setAmount(multi.<BigDecimal>getAttribute(CIPayroll.PositionAbstract.Amount))
                                .setEmployee(multi.getMsgPhrase(selDoc, msgPhrase))
                                .setDepartment(multi.<String>getSelect(selDepName));
                datasource.add(bean);
            }
            return new JRBeanCollectionDataSource(datasource);
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
            final Map<String, Object> filterMap = getFilterReport().getFilterMap(_parameter);
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
            if (filterMap.containsKey("status")) {
                final StatusFilterValue filter = (StatusFilterValue) filterMap.get("status");
                if (!filter.getObject().isEmpty()) {
                    attrQueryBldr.addWhereAttrEqValue(CIPayroll.DocumentAbstract.StatusAbstract,
                                    filter.getObject().toArray());
                }
            }
            if (filterMap.containsKey("employee")) {
                final InstanceFilterValue filter = (InstanceFilterValue) filterMap.get("employee");
                if (filter.getObject() != null && filter.getObject().isValid()) {
                    attrQueryBldr.addWhereAttrEqValue(CIPayroll.DocumentAbstract.EmployeeAbstractLink,
                                    filter.getObject());
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
            final CrosstabBuilder crosstab = DynamicReports.ctab.crosstab();
            final Map<String, Object> filterMap = getFilterReport().getFilterMap(_parameter);

            if (filterMap.containsKey("departmentGroup")) {
                if (BooleanUtils.isTrue((Boolean) filterMap.get("departmentGroup"))) {
                    final CrosstabRowGroupBuilder<String> rowGroup = DynamicReports.ctab.rowGroup("department",
                                    String.class).setHeaderWidth(150);
                    crosstab.addRowGroup(rowGroup);
                }
            }

            final CrosstabMeasureBuilder<BigDecimal> amountMeasure = DynamicReports.ctab.measure(
                            "amount", BigDecimal.class, Calculation.SUM);
            crosstab.addMeasure(amountMeasure);

            final CrosstabRowGroupBuilder<String> rowGroup = DynamicReports.ctab.rowGroup("document", String.class)
                            .setHeaderWidth(150);
            crosstab.addRowGroup(rowGroup);

            final CrosstabColumnGroupBuilder<String> columnGroup = DynamicReports.ctab.columnGroup("position",
                            String.class);

            crosstab.addColumnGroup(columnGroup);

            _builder.addSummary(crosstab);
        }

        /**
         * Getter method for the instance variable {@link #filterReport}.
         *
         * @return value of instance variable {@link #filterReport}
         */
        public PositionAnalyzeReport_Base getFilterReport()
        {
            return this.filterReport;
        }
    }

    public static class DataBean
    {

        private Boolean switched;
        private String department;
        private String docName;
        private Instance docInst;
        private Instance posInst;
        private String posKey;
        private String posDescr;
        private BigDecimal amount;
        private String employee;

        /**
         * Getter method for the instance variable {@link #document}.
         *
         * @return value of instance variable {@link #document}
         */
        public String getDocument()
        {
            return getDocName() + " - " + getEmployee();
        }

        /**
         * Getter method for the instance variable {@link #document}.
         *
         * @return value of instance variable {@link #document}
         */
        public String getPosition()
        {
            String ret;
            if (getSwitched()) {
                ret = getPosKey() + " " + getPosDescr();
            } else {
                ret = getPosInst().getType().getLabel();
            }
            return ret;
        }

        /**
         * Getter method for the instance variable {@link #docInst}.
         *
         * @return value of instance variable {@link #docInst}
         */
        public Instance getDocInst()
        {
            return this.docInst;
        }

        /**
         * Setter method for instance variable {@link #docInst}.
         *
         * @param _docInst value for instance variable {@link #docInst}
         * @return this for chaining
         */
        public DataBean setDocInst(final Instance _docInst)
        {
            this.docInst = _docInst;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #docName}.
         *
         * @return value of instance variable {@link #docName}
         */
        public String getDocName()
        {
            return this.docName;
        }

        /**
         * Setter method for instance variable {@link #docName}.
         *
         * @param _docName value for instance variable {@link #docName}
         * @return this for chaining
         */
        public DataBean setDocName(final String _docName)
        {
            this.docName = _docName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #posKey}.
         *
         * @return value of instance variable {@link #posKey}
         */
        public String getPosKey()
        {
            return this.posKey;
        }

        /**
         * Setter method for instance variable {@link #posKey}.
         *
         * @param _posKey value for instance variable {@link #posKey}
         * @return this for chaining
         */
        public DataBean setPosKey(final String _posKey)
        {
            this.posKey = _posKey;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #posDescr}.
         *
         * @return value of instance variable {@link #posDescr}
         */
        public String getPosDescr()
        {
            return this.posDescr;
        }

        /**
         * Setter method for instance variable {@link #posDescr}.
         *
         * @param _posDescr value for instance variable {@link #posDescr}
         * @return this for chaining
         */
        public DataBean setPosDescr(final String _posDescr)
        {
            this.posDescr = _posDescr;
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
         * @return this for chaining
         */
        public DataBean setAmount(final BigDecimal _amount)
        {
            this.amount = _amount;
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
         * @return this for chaining
         */
        public DataBean setEmployee(final String _employee)
        {
            this.employee = _employee;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #posInst}.
         *
         * @return value of instance variable {@link #posInst}
         */
        public Instance getPosInst()
        {
            return this.posInst;
        }

        /**
         * Setter method for instance variable {@link #posInst}.
         *
         * @param _posInst value for instance variable {@link #posInst}
         * @return this for chaining
         */
        public DataBean setPosInst(final Instance _posInst)
        {
            this.posInst = _posInst;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #switched}.
         *
         * @return value of instance variable {@link #switched}
         */
        public Boolean getSwitched()
        {
            return this.switched;
        }

        /**
         * Setter method for instance variable {@link #switched}.
         *
         * @param _switched value for instance variable {@link #switched}
         * @return this for chaining
         */
        public DataBean setSwitched(final Boolean _switched)
        {
            this.switched = _switched;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #department}.
         *
         * @return value of instance variable {@link #department}
         */
        public String getDepartment()
        {
            return this.department;
        }

        /**
         * Setter method for instance variable {@link #department}.
         *
         * @param _department value for instance variable {@link #department}
         * @return this for chaining
         */
        public DataBean setDepartment(final String _department)
        {
            this.department = _department;
            return this;
        }
    }

}
