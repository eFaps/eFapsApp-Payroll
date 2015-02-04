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

import static net.sf.dynamicreports.report.builder.DynamicReports.type;

import java.awt.Color;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.base.expression.AbstractSimpleExpression;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnGridComponentBuilder;
import net.sf.dynamicreports.report.builder.grid.ColumnTitleGroupBuilder;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.style.StyleBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.dynamicreports.report.definition.ReportParameters;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang.BooleanUtils;
import org.efaps.admin.common.MsgPhrase;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
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
import org.efaps.esjp.ci.CIProjects;
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

        /**
         * Filtered Report.
         */
        private final PositionAnalyzeReport_Base filterReport;

        /**
         * Data for the positions.
         */
        private Map<Instance, Map<String, Object>> data;

        /**
         * List of columns.
         */
        private List<Column> columns;

        /**
         * @param _filterReport report
         */
        public DynPositionAnalyzeReport(final PositionAnalyzeReport_Base _filterReport)
        {
            this.filterReport = _filterReport;
        }

        @Override
        protected StyleBuilder getColumnStyle4Html(final Parameter _parameter)
            throws EFapsException
        {
            return super.getColumnStyle4Html(_parameter).setBorder(DynamicReports.stl.pen1Point());
        }

        /**
         * Getter method for the instance variable {@link #data}.
         *
         * @param _parameter Paramdeter as passed by the eFaps API
         * @return value of instance variable {@link #data}
         * @throws EFapsException on error
         */
        public Map<Instance, Map<String, Object>> getData(final Parameter _parameter)
            throws EFapsException
        {
            if (this.data == null) {
                this.data = new HashMap<>();
                final Map<String, Column> columnMap = new HashMap<>();
                final Map<String, Object> filterMap = getFilterReport().getFilterMap(_parameter);
                boolean project = false;
                if (filterMap.containsKey("projectGroup")) {
                    project = BooleanUtils.isTrue((Boolean) filterMap.get("projectGroup"));
                }
                //Payroll_Employee4DocumentMsgPhrase
                final MsgPhrase msgPhrase = MsgPhrase.get(UUID.fromString("4bf03526-3616-4e57-ad70-1e372029ea9e"));
                MsgPhrase msgPhrase4Project = null;
                if (project) {
                    // Project_ProjectMsgPhrase
                    msgPhrase4Project = MsgPhrase.get(UUID
                                    .fromString("64c30826-cb22-4579-a3d5-bd10090f155e"));
                }

                boolean showDetails = true;
                if (filterMap.containsKey("switch")) {
                    showDetails = BooleanUtils.isTrue((Boolean) filterMap.get("switch"));
                }

                final QueryBuilder queryBuilder = new QueryBuilder(CIPayroll.PositionDeduction);
                queryBuilder.addType(CIPayroll.PositionNeutral, CIPayroll.PositionPayment);
                add2QueryBuilder(_parameter, queryBuilder);
                final MultiPrintQuery multi = queryBuilder.getPrint();
                final SelectBuilder selDoc = SelectBuilder.get()
                                .linkto(CIPayroll.PositionAbstract.DocumentAbstractLink);
                final SelectBuilder selDocInst = new SelectBuilder(selDoc).instance();
                final SelectBuilder selDocName = new SelectBuilder(selDoc).attribute(CIPayroll.DocumentAbstract.Name);
                final SelectBuilder selCrossTotal = new SelectBuilder(selDoc)
                                .attribute(CIPayroll.DocumentAbstract.CrossTotal);
                final SelectBuilder selAmountCost = new SelectBuilder(selDoc)
                                .attribute(CIPayroll.DocumentAbstract.AmountCost);
                final SelectBuilder selDocELT = new SelectBuilder(selDoc)
                                .attribute(CIPayroll.DocumentAbstract.ExtraLaborTime);
                final SelectBuilder selDocHLT = new SelectBuilder(selDoc)
                                .attribute(CIPayroll.DocumentAbstract.HolidayLaborTime);
                final SelectBuilder selDocLT = new SelectBuilder(selDoc)
                                .attribute(CIPayroll.DocumentAbstract.LaborTime);
                final SelectBuilder selDocNLT = new SelectBuilder(selDoc)
                                .attribute(CIPayroll.DocumentAbstract.NightLaborTime);
                final SelectBuilder selDepName = new SelectBuilder(selDoc)
                                .linkto(CIPayroll.DocumentAbstract.EmployeeAbstractLink)
                                .linkfrom(CIHumanResource.Department2EmployeeAdminister.EmployeeLink)
                                .linkto(CIHumanResource.Department2EmployeeAdminister.DepartmentLink)
                                .attribute(CIHumanResource.Department.Name);
                final SelectBuilder selRemun = new SelectBuilder(selDoc)
                                .linkto(CIPayroll.DocumentAbstract.EmployeeAbstractLink)
                                .clazz(CIHumanResource.ClassTR_Labor)
                                .attribute(CIHumanResource.ClassTR_Labor.Remuneration);
                final SelectBuilder selProj = new SelectBuilder(selDoc)
                                .linkfrom(CIProjects.Project2DocumentAbstract.ToAbstract)
                                .linkto(CIProjects.Project2DocumentAbstract.FromAbstract);
                final SelectBuilder selEmplNum = new SelectBuilder(selDoc)
                                .linkto(CIPayroll.DocumentAbstract.EmployeeAbstractLink)
                                .attribute(CIHumanResource.Employee.Number);
                if (project) {
                    multi.addMsgPhrase(selProj, msgPhrase4Project);
                }
                multi.addMsgPhrase(selDoc, msgPhrase);
                multi.addSelect(selCrossTotal, selAmountCost, selDocInst, selDocName, selDepName, selDocELT, selDocHLT,
                                selDocLT, selDocNLT, selRemun, selEmplNum);
                multi.addAttribute(CIPayroll.PositionAbstract.Amount, CIPayroll.PositionAbstract.Description,
                                CIPayroll.PositionAbstract.Key);
                multi.execute();
                while (multi.next()) {
                    final Instance docInst = multi.getSelect(selDocInst);
                    Map<String, Object> map;
                    if (this.data.containsKey(docInst)) {
                        map = this.data.get(docInst);
                    } else {
                        map = new HashMap<>();
                        this.data.put(docInst, map);
                        map.put("Remuneration", multi.getSelect(selRemun));
                        map.put(CIPayroll.DocumentAbstract.Name.name, multi.getSelect(selDocName));
                        map.put(CIPayroll.DocumentAbstract.CrossTotal.name, multi.getSelect(selCrossTotal));
                        map.put(CIPayroll.DocumentAbstract.AmountCost.name, multi.getSelect(selAmountCost));
                        final BigDecimal laborTime =  (BigDecimal) multi.<Object[]>getSelect(selDocLT)[0];
                        if (showDetails) {
                            map.put(CIPayroll.DocumentAbstract.LaborTime.name, laborTime);
                        } else {
                            map.put(CIPayroll.DocumentAbstract.LaborTime.name, laborTime.divide(BigDecimal.valueOf(8),
                                            BigDecimal.ROUND_HALF_UP));
                        }
                        map.put(CIPayroll.DocumentAbstract.ExtraLaborTime.name,
                                        multi.<Object[]>getSelect(selDocELT)[0]);
                        map.put(CIPayroll.DocumentAbstract.HolidayLaborTime.name,
                                        multi.<Object[]>getSelect(selDocHLT)[0]);
                        map.put(CIPayroll.DocumentAbstract.NightLaborTime.name,
                                        multi.<Object[]>getSelect(selDocNLT)[0]);
                        map.put(CIPayroll.DocumentAbstract.EmployeeAbstractLink.name,
                                        multi.getMsgPhrase(selDoc, msgPhrase));
                        map.put("Department", multi.getSelect(selDepName));
                        map.put("EmployeeNumber", multi.getSelect(selEmplNum));
                        if (project) {
                            map.put("Project", multi.getMsgPhrase(selProj, msgPhrase4Project));
                        }
                        PositionAnalyzeReport_Base.LOG.debug("Read: {}", map);
                    }

                    if (showDetails) {
                        final String key = multi.getAttribute(CIPayroll.PositionAbstract.Key);
                        if (!columnMap.containsKey(key)) {
                            final Column col = new Column();
                            columnMap.put(key, col);
                            col.setKey(key);
                            final String descr = multi.getAttribute(CIPayroll.PositionAbstract.Description);
                            col.setLabel(key + " - " + descr);
                            col.setGroup(multi.getCurrentInstance().getType().getLabel());
                        }
                        map.put(key, multi.getAttribute(CIPayroll.PositionAbstract.Amount));
                    } else {
                        final String key = multi.getCurrentInstance().getType().getName();
                        if (map.containsKey(key)) {
                            map.put(key, ((BigDecimal) map.get(key)).add(multi
                                            .<BigDecimal>getAttribute(CIPayroll.PositionAbstract.Amount)));
                        } else {
                            final Column col = new Column();
                            columnMap.put(key, col);
                            col.setKey(key);
                            col.setLabel(multi.getCurrentInstance().getType().getLabel());
                            col.setGroup(multi.getCurrentInstance().getType().getLabel());
                            map.put(key, multi.getAttribute(CIPayroll.PositionAbstract.Amount));
                        }
                    }
                }
                // Artificially add the one that do not have positions (happens with manually created advances)
                final QueryBuilder docQueryBldr = getAttrQueryBuilder(_parameter);
                final QueryBuilder attrQueryBuilder = new QueryBuilder(CIPayroll.PositionDeduction);
                attrQueryBuilder.addType(CIPayroll.PositionNeutral, CIPayroll.PositionPayment);
                docQueryBldr.addWhereAttrNotInQuery(CIPayroll.DocumentAbstract.ID,
                                attrQueryBuilder.getAttributeQuery(CIPayroll.PositionAbstract.DocumentAbstractLink));
                final MultiPrintQuery docMulti = docQueryBldr.getPrint();
                final SelectBuilder selRemun4Doc = SelectBuilder.get()
                                .linkto(CIPayroll.DocumentAbstract.EmployeeAbstractLink)
                                .clazz(CIHumanResource.ClassTR_Labor)
                                .attribute(CIHumanResource.ClassTR_Labor.Remuneration);
                final SelectBuilder selProj4Doc = SelectBuilder.get()
                                .linkfrom(CIProjects.Project2DocumentAbstract.ToAbstract)
                                .linkto(CIProjects.Project2DocumentAbstract.FromAbstract);
                final SelectBuilder selEmplNum4Doc = SelectBuilder.get()
                                .linkto(CIPayroll.DocumentAbstract.EmployeeAbstractLink)
                                .attribute(CIHumanResource.Employee.Number);
                final SelectBuilder selDepName4Doc = SelectBuilder.get()
                                .linkto(CIPayroll.DocumentAbstract.EmployeeAbstractLink)
                                .linkfrom(CIHumanResource.Department2EmployeeAdminister.EmployeeLink)
                                .linkto(CIHumanResource.Department2EmployeeAdminister.DepartmentLink)
                                .attribute(CIHumanResource.Department.Name);
                if (project) {
                    multi.addMsgPhrase(selProj4Doc, msgPhrase4Project);
                }
                docMulti.addMsgPhrase(msgPhrase);
                docMulti.addSelect(selDepName4Doc, selRemun4Doc, selEmplNum4Doc);

                docMulti.addAttribute(CIPayroll.DocumentAbstract.Name, CIPayroll.DocumentAbstract.AmountCost,
                                CIPayroll.DocumentAbstract.CrossTotal, CIPayroll.DocumentAbstract.LaborTime,
                                CIPayroll.DocumentAbstract.ExtraLaborTime, CIPayroll.DocumentAbstract.NightLaborTime,
                                CIPayroll.DocumentAbstract.HolidayLaborTime);
                docMulti.execute();
                while (docMulti.next()) {
                    final Map<String, Object> map = new HashMap<>();
                    this.data.put(docMulti.getCurrentInstance(), map);
                    map.put("Remuneration", docMulti.getSelect(selRemun4Doc));
                    map.put(CIPayroll.DocumentAbstract.Name.name,
                                    docMulti.getAttribute(CIPayroll.DocumentAbstract.Name));
                    map.put(CIPayroll.DocumentAbstract.CrossTotal.name,
                                    docMulti.getAttribute(CIPayroll.DocumentAbstract.CrossTotal));
                    map.put(CIPayroll.DocumentAbstract.AmountCost.name,
                                    docMulti.getAttribute(CIPayroll.DocumentAbstract.AmountCost));
                    final BigDecimal laborTime = (BigDecimal) docMulti
                                    .<Object[]>getAttribute(CIPayroll.DocumentAbstract.LaborTime)[0];
                    if (showDetails) {
                        map.put(CIPayroll.DocumentAbstract.LaborTime.name, laborTime);
                    } else {
                        map.put(CIPayroll.DocumentAbstract.LaborTime.name, laborTime.divide(BigDecimal.valueOf(8),
                                        BigDecimal.ROUND_HALF_UP));
                    }
                    map.put(CIPayroll.DocumentAbstract.ExtraLaborTime.name,
                                    docMulti.<Object[]>getAttribute(CIPayroll.DocumentAbstract.ExtraLaborTime)[0]);
                    map.put(CIPayroll.DocumentAbstract.HolidayLaborTime.name,
                                    docMulti.<Object[]>getAttribute(CIPayroll.DocumentAbstract.HolidayLaborTime)[0]);
                    map.put(CIPayroll.DocumentAbstract.NightLaborTime.name,
                                    docMulti.<Object[]>getAttribute(CIPayroll.DocumentAbstract.NightLaborTime)[0]);
                    map.put(CIPayroll.DocumentAbstract.EmployeeAbstractLink.name,
                                    docMulti.getMsgPhrase(msgPhrase));
                    map.put("Department", docMulti.getSelect(selDepName4Doc));
                    map.put("EmployeeNumber", docMulti.getSelect(selEmplNum4Doc));
                    if (project) {
                        map.put("Project", docMulti.getMsgPhrase(selProj4Doc, msgPhrase4Project));
                    }
                }
                this.columns = new ArrayList<>(columnMap.values());
                Collections.sort(this.columns, new Comparator<Column>()
                {

                    @Override
                    public int compare(final Column _arg0,
                                       final Column _arg1)
                    {
                        return _arg0.getLabel().compareTo(_arg1.getLabel());
                    }
                });
            }
            return this.data;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filterMap = getFilterReport().getFilterMap(_parameter);
            if (filterMap.containsKey("switch")) {
                BooleanUtils.isTrue((Boolean) filterMap.get("switch"));
            }

            final Map<Instance, Map<String, Object>> maps = getData(_parameter);
            final List<Map<String, Object>> values = new ArrayList<>(maps.values());
            final ComparatorChain<Map<String, Object>> comp = new ComparatorChain<>();

            if (filterMap.containsKey("projectGroup")) {
                if (BooleanUtils.isTrue((Boolean) filterMap.get("projectGroup"))) {
                    comp.addComparator(new Comparator<Map<String, Object>>()
                    {

                        @Override
                        public int compare(final Map<String, Object> _o1,
                                           final Map<String, Object> _o2)
                        {
                            final String str1 = _o1.containsKey("Project") && _o1.get("Project") != null ? (String) _o1
                                            .get("Project") : "";
                            final String str2 = _o2.containsKey("Project") && _o2.get("Project") != null ? (String) _o2
                                            .get("Project") : "";
                            return str1.compareTo(str2);
                        }
                    });
                }
            }

            if (filterMap.containsKey("departmentGroup")) {
                if (BooleanUtils.isTrue((Boolean) filterMap.get("departmentGroup"))) {
                    comp.addComparator(new Comparator<Map<String, Object>>()
                    {

                        @Override
                        public int compare(final Map<String, Object> _o1,
                                           final Map<String, Object> _o2)
                        {
                            final String str1 = _o1.containsKey("Department") && _o1.get("Department") != null
                                            ? (String) _o1.get("Department") : "";
                            final String str2 = _o2.containsKey("Department") && _o2.get("Department") != null
                                            ? (String) _o2.get("Department") : "";
                            return str1.compareTo(str2);
                        }
                    });
                }
            }
            comp.addComparator(new Comparator<Map<String, Object>>()
            {

                @Override
                public int compare(final Map<String, Object> _o1,
                                   final Map<String, Object> _o2)
                {
                    final String str1 = _o1.containsKey("EmployeeAbstractLink")
                                    && _o1.get("EmployeeAbstractLink") != null ? (String) _o1
                                    .get("EmployeeAbstractLink") : "";
                    final String str2 = _o2.containsKey("EmployeeAbstractLink")
                                    && _o2.get("EmployeeAbstractLink") != null ? (String) _o2
                                    .get("EmployeeAbstractLink") : "";
                    return str1.compareTo(str2);
                }
            });

            comp.addComparator(new Comparator<Map<String, Object>>()
            {

                @Override
                public int compare(final Map<String, Object> _o1,
                                   final Map<String, Object> _o2)
                {
                    final String str1 = _o1.containsKey("Name") && _o1.get("Name") != null ? (String) _o1
                                    .get("Name") : "";
                    final String str2 = _o2.containsKey("Name") && _o2.get("Name") != null ? (String) _o2
                                    .get("Name") : "";
                    return str1.compareTo(str2);
                }
            });

            Collections.sort(values, comp);
            return new JRMapCollectionDataSource(new ArrayList<Map<String, ?>>(values));
        }

        /**
         * @param _parameter Parameter as passed by the eFaps API
         * @throws EFapsException on error
         * @return QueryBuilder
         */
        protected QueryBuilder getAttrQueryBuilder(final Parameter _parameter)
            throws EFapsException
        {
            final Map<String, Object> filterMap = getFilterReport().getFilterMap(_parameter);
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
                    // the documents have the same status keys but must be selected
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

        /**
         * @param _parameter Parameter as passed by the eFaps API
         * @param _queryBldr queryBldr to add to
         * @throws EFapsException on error
         */
        protected void add2QueryBuilder(final Parameter _parameter,
                                        final QueryBuilder _queryBldr)
            throws EFapsException
        {
            _queryBldr.addWhereAttrInQuery(CIPayroll.PositionAbstract.DocumentAbstractLink,
                                getAttrQueryBuilder(_parameter).getAttributeQuery(CISales.DocumentAbstract.ID));
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            final Map<String, Object> filterMap = getFilterReport().getFilterMap(_parameter);
            final List<ColumnGridComponentBuilder> groups = new ArrayList<>();

            ColumnGroupBuilder projectGroup = null;
            if (filterMap.containsKey("projectGroup")) {
                if (BooleanUtils.isTrue((Boolean) filterMap.get("projectGroup"))) {
                    final TextColumnBuilder<String> col = DynamicReports.col.column(getLabel("Project"),
                                    "Project", DynamicReports.type.stringType())
                                    .setStyle(DynamicReports.stl.style().setBackgroundColor(Color.yellow));
                    _builder.addColumn(col);
                    projectGroup = DynamicReports.grp.group(col);
                    _builder.addGroup(projectGroup);
                }
            }
            ColumnGroupBuilder departmentGroup = null;
            if (filterMap.containsKey("departmentGroup")) {
                if (BooleanUtils.isTrue((Boolean) filterMap.get("departmentGroup"))) {
                    final TextColumnBuilder<String> col = DynamicReports.col.column(getLabel("Department"),
                                    "Department", DynamicReports.type.stringType());
                    _builder.addColumn(col);
                    departmentGroup = DynamicReports.grp.group(col);
                    _builder.addGroup(departmentGroup);
                }
            }
            boolean showDetails = true;
            if (filterMap.containsKey("switch")) {
                showDetails = BooleanUtils.isTrue((Boolean) filterMap.get("switch"));
            }

            final TextColumnBuilder<String> nameCol = DynamicReports.col.column(getLabel("Name"),
                            CIPayroll.DocumentAbstract.Name.name, DynamicReports.type.stringType());
            final TextColumnBuilder<String> employeeCol = DynamicReports.col.column(getLabel("Employee"),
                            CIPayroll.DocumentAbstract.EmployeeAbstractLink.name, DynamicReports.type.stringType())
                            .setWidth(200);
            final TextColumnBuilder<String> employeeNumCol = DynamicReports.col.column(getLabel("EmployeeNumber"),
                            "EmployeeNumber", DynamicReports.type.stringType());
            _builder.addColumn(nameCol, employeeCol, employeeNumCol);
            groups.add(nameCol);
            groups.add(employeeCol);
            groups.add(employeeNumCol);

            final TextColumnBuilder<BigDecimal> remCol = DynamicReports.col.column(getLabel("Remuneration"),
                            "Remuneration", DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> ltCol;
            if (showDetails) {
                ltCol = DynamicReports.col.column(getLabel("LaborTime"),
                                CIPayroll.DocumentAbstract.LaborTime.name, DynamicReports.type.bigDecimalType());
            } else {
                ltCol = DynamicReports.col.column(getLabel("LaborTimeDays"),
                            CIPayroll.DocumentAbstract.LaborTime.name, DynamicReports.type.bigDecimalType());
            }
            final TextColumnBuilder<BigDecimal> eltCol = DynamicReports.col.column(getLabel("ExtraLaborTime"),
                            CIPayroll.DocumentAbstract.ExtraLaborTime.name, DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> nltCol = DynamicReports.col.column(getLabel("NightLaborTime"),
                            CIPayroll.DocumentAbstract.NightLaborTime.name, DynamicReports.type.bigDecimalType());
            final TextColumnBuilder<BigDecimal> hltCol = DynamicReports.col.column(getLabel("HolidayLaborTime"),
                            CIPayroll.DocumentAbstract.HolidayLaborTime.name, DynamicReports.type.bigDecimalType());
            _builder.addColumn(remCol, ltCol, eltCol, nltCol, hltCol);
            groups.add(remCol);
            groups.add(ltCol);
            groups.add(eltCol);
            groups.add(nltCol);
            groups.add(hltCol);

            final Map<String, ColumnTitleGroupBuilder> groupMap = new LinkedHashMap<>();
            groupMap.put(CIPayroll.PositionPayment.getType().getLabel(),
                            DynamicReports.grid.titleGroup(CIPayroll.PositionPayment.getType().getLabel()));
            groupMap.put(CIPayroll.PositionDeduction.getType().getLabel(),
                            DynamicReports.grid.titleGroup(CIPayroll.PositionDeduction.getType().getLabel()));
            groupMap.put(CIPayroll.PositionNeutral.getType().getLabel(),
                            DynamicReports.grid.titleGroup(CIPayroll.PositionNeutral.getType().getLabel()));

            final Map<String, Set<String>> keyMap = new HashMap<>();
            for (final Column column : getColumns(_parameter)) {
                final TextColumnBuilder<BigDecimal> column1 = DynamicReports.col.column(column.getLabel(),
                                column.getKey(), DynamicReports.type.bigDecimalType());
                if (showDetails) {
                    column1.setTitleHeight(100);
                    Set<String> keySet;
                    if (keyMap.containsKey(column.getGroup())) {
                        keySet = keyMap.get(column.getGroup());
                    } else {
                        keySet = new HashSet<>();
                        keyMap.put(column.getGroup(), keySet);
                    }
                    keySet.add(column.getKey());
                }
                _builder.addColumn(column1);
                if (groupMap.containsKey(column.getGroup())) {
                    groupMap.get(column.getGroup()).add(column1);
                }
                if (departmentGroup != null) {
                    final AggregationSubtotalBuilder<BigDecimal> sum = DynamicReports.sbt.sum(column1);
                    _builder.addSubtotalAtGroupFooter(departmentGroup, sum);
                }
                if (projectGroup != null) {
                    final AggregationSubtotalBuilder<BigDecimal> sum = DynamicReports.sbt.sum(column1);
                    _builder.addSubtotalAtGroupFooter(projectGroup, sum);
                }
                final AggregationSubtotalBuilder<BigDecimal> sum = DynamicReports.sbt.sum(column1);
                _builder.addSubtotalAtColumnFooter(sum);
            }

            final TextColumnBuilder<BigDecimal> crossCol = DynamicReports.col.column(getLabel("CrossTotal"),
                            CIPayroll.DocumentAbstract.CrossTotal.name, DynamicReports.type.bigDecimalType());

            final TextColumnBuilder<BigDecimal> amountCol = DynamicReports.col.column(getLabel("AmountCost"),
                            CIPayroll.DocumentAbstract.AmountCost.name, DynamicReports.type.bigDecimalType());
            _builder.addColumn(crossCol, amountCol);

            if (showDetails) {
                for (final Entry<String, ColumnTitleGroupBuilder> entry : groupMap.entrySet()) {
                    if (!entry.getValue().getColumnGridTitleGroup().getList().getListCells().isEmpty()) {
                        final TextColumnBuilder<BigDecimal> col = DynamicReports.col.column(new SumExpression(
                                        keyMap.get(entry.getKey()))).setDataType(type.bigDecimalType())
                                        .setTitle(getLabel("Total"));
                        _builder.addColumn(col);
                        entry.getValue().add(col);
                        groups.add(entry.getValue());

                        if (departmentGroup != null) {
                            final AggregationSubtotalBuilder<BigDecimal> colTotal = DynamicReports.sbt.sum(col);
                            _builder.addSubtotalAtGroupFooter(departmentGroup, colTotal);
                        }
                        if (projectGroup != null) {
                            final AggregationSubtotalBuilder<BigDecimal> colTotal = DynamicReports.sbt.sum(col);
                            _builder.addSubtotalAtGroupFooter(projectGroup, colTotal);
                        }
                        final AggregationSubtotalBuilder<BigDecimal> colTotal = DynamicReports.sbt.sum(col);
                        _builder.addSubtotalAtColumnFooter(colTotal);
                    }
                }
                groups.add(crossCol);
                groups.add(amountCol);
                _builder.columnGrid(groups.toArray(new ColumnGridComponentBuilder[groups.size()]));
            }

            if (departmentGroup != null) {
                final AggregationSubtotalBuilder<BigDecimal> crossTotal = DynamicReports.sbt.sum(crossCol);
                final AggregationSubtotalBuilder<BigDecimal> amountTotal = DynamicReports.sbt.sum(amountCol);
                _builder.addSubtotalAtGroupFooter(departmentGroup, crossTotal);
                _builder.addSubtotalAtGroupFooter(departmentGroup, amountTotal);
            }
            if (projectGroup != null) {
                final AggregationSubtotalBuilder<BigDecimal> crossTotal = DynamicReports.sbt.sum(crossCol);
                final AggregationSubtotalBuilder<BigDecimal> amountTotal = DynamicReports.sbt.sum(amountCol);
                _builder.addSubtotalAtGroupFooter(projectGroup, crossTotal);
                _builder.addSubtotalAtGroupFooter(projectGroup, amountTotal);
            }
            final AggregationSubtotalBuilder<BigDecimal> crossTotal = DynamicReports.sbt.sum(crossCol);
            final AggregationSubtotalBuilder<BigDecimal> amountTotal = DynamicReports.sbt.sum(amountCol);

            _builder.addSubtotalAtColumnFooter(crossTotal);
            _builder.addSubtotalAtColumnFooter(amountTotal);
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

        /**
         * @param _key key the label is wanted for
         * @return label
         */
        protected String getLabel(final String _key)
        {
            return DBProperties.getProperty(PositionAnalyzeReport.class.getName() + "." + _key);
        }

        /**
         * Getter method for the instance variable {@link #columns}.
         *
         * @param _parameter Parameter as passed by the eFaps API
         * @return value of instance variable {@link #columns}
         * @throws EFapsException on error
         */
        public List<Column> getColumns(final Parameter _parameter)
            throws EFapsException
        {
            if (this.columns == null) {
                getData(_parameter);
            }
            return this.columns;
        }
    }

    /**
     * Column class.
     */
    public static class Column
    {

        /**
         * Key.
         */
        private String key;

        /**
         * Label.
         */
        private String label;

        /**
         * Group.
         */
        private String group;

        /**
         * Getter method for the instance variable {@link #key}.
         *
         * @return value of instance variable {@link #key}
         */
        public String getKey()
        {
            return this.key;
        }

        /**
         * Setter method for instance variable {@link #key}.
         *
         * @param _key value for instance variable {@link #key}
         */
        public void setKey(final String _key)
        {
            this.key = _key;
        }

        /**
         * Getter method for the instance variable {@link #label}.
         *
         * @return value of instance variable {@link #label}
         */
        public String getLabel()
        {
            return this.label;
        }

        /**
         * Setter method for instance variable {@link #label}.
         *
         * @param _label value for instance variable {@link #label}
         */
        public void setLabel(final String _label)
        {
            this.label = _label;
        }

        @Override
        public boolean equals(final Object _obj)
        {
            boolean ret;
            if (_obj instanceof Column) {
                ret = getKey().equals(((Column) _obj).getKey());
            } else {
                ret = super.equals(_obj);
            }
            return ret;
        }

        @Override
        public int hashCode()
        {
            return super.hashCode();
        }

        /**
         * Getter method for the instance variable {@link #group}.
         *
         * @return value of instance variable {@link #group}
         */
        public String getGroup()
        {
            return this.group;
        }

        /**
         * Setter method for instance variable {@link #group}.
         *
         * @param _group value for instance variable {@link #group}
         */
        public void setGroup(final String _group)
        {
            this.group = _group;
        }
    }

    /**
     * Expression to get the sum.
     */
    public static class SumExpression
        extends AbstractSimpleExpression<BigDecimal>
    {

        /**
         * Needed for serialization.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Set of keys.
         */
        private final Set<String> keys;

        /**
         * @param _keys key set
         */
        public SumExpression(final Set<String> _keys)
        {
            this.keys = _keys;
        }

        @Override
        public BigDecimal evaluate(final ReportParameters _reportParameters)
        {
            BigDecimal ret = BigDecimal.ZERO;
            for (final String key : this.keys) {
                final Object value = _reportParameters.getFieldValue(key);
                if (value != null && value instanceof BigDecimal) {
                    ret = ret.add((BigDecimal) value);
                }
            }
            return ret;
        }
    }
}
