/*
 * Copyright 2003 - 2013 The eFaps Team
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
 * Revision:        $Rev: 295 $
 * Last Changed:    $Date: 2011-06-06 15:37:52 -0500 (lun, 06 jun 2011) $
 * Last Changed By: $Author: Jorge Cueva $
 */
package org.efaps.esjp.payroll.reports;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.jasperreports.engine.JRDataSource;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.admin.ui.field.Field.Display;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIFormPayroll;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.jasperreport.EFapsTextReport;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.common.uiform.Field_Base.DropDownPosition;
import org.efaps.esjp.payroll.util.Payroll;
import org.efaps.esjp.payroll.util.PayrollSettings;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: WorkOrderCalibrateDataSource.java 268 2011-04-29 17:10:40Z Jorge Cueva $
 */
@EFapsUUID("bb288739-7ecc-498b-9633-f32f21978999")
@EFapsRevision("$Rev: 295 $")
public abstract class ExportAFPReport_Base
    extends AbstractReports
{
    public enum Mime {
        TXT,
        XLS,
        PDF
    }

    private static final Map<String, DropDownPosition> REMUTYPE_MAP = new HashMap<String, DropDownPosition>();
    static {
        ExportAFPReport_Base.REMUTYPE_MAP.put("0", new DropDownPosition("", ""));
        ExportAFPReport_Base.REMUTYPE_MAP.put("I", new DropDownPosition("I",
                        DBProperties.getProperty("org.efaps.esjp.payroll.reports.ExportAFPReport.RemuI")));
        ExportAFPReport_Base.REMUTYPE_MAP.put("J", new DropDownPosition("J",
                        DBProperties.getProperty("org.efaps.esjp.payroll.reports.ExportAFPReport.RemuJ")));
        ExportAFPReport_Base.REMUTYPE_MAP.put("K", new DropDownPosition("K",
                        DBProperties.getProperty("org.efaps.esjp.payroll.reports.ExportAFPReport.RemuK")));
        ExportAFPReport_Base.REMUTYPE_MAP.put("L", new DropDownPosition("L",
                        DBProperties.getProperty("org.efaps.esjp.payroll.reports.ExportAFPReport.RemuL")));
    }

    public Return dropDownAfpRemuType(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final List<DropDownPosition> lst = new ArrayList<DropDownPosition>();
        lst.addAll(ExportAFPReport_Base.REMUTYPE_MAP.values());
        final StringBuilder html = new Field().getDropDownField(_parameter, lst);
        ret.put(ReturnValues.SNIPLETT, html);
        return ret;
    }

    public Return formatAfpRemuType(final Parameter _parameter)
        throws EFapsException
    {
        final TargetMode mode = (TargetMode) _parameter.get(ParameterValues.ACCESSMODE);
        final StringBuilder js = new StringBuilder();
        final Return retVal = new Return();
        if (mode.equals(TargetMode.VIEW) || mode.equals(TargetMode.PRINT)) {
            final FieldValue keyField = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
            if (keyField.getDisplay().equals(Display.READONLY)) {
                final String key = (String) keyField.getValue();
                if (ExportAFPReport_Base.REMUTYPE_MAP.containsKey(key)) {
                    final String value = (String) ExportAFPReport_Base.REMUTYPE_MAP.get(key).getOption();
                    js.append(key)
                        .append(" - ")
                        .append(value);
                }
            } else {
                js.append(keyField.getValue());
            }
            retVal.put(ReturnValues.VALUES, js.toString());
        }
        return retVal;
    }

    public Return createReport(final Parameter _parameter)
        throws EFapsException
    {
        Return ret = new Return();
        final String mime = _parameter.getParameterValue(CIFormPayroll.Payroll_ExportAFPReportForm.mime.name);
        final Mime def = Mime.valueOf(mime.toUpperCase());
        AbstractDynamicReport dyRp = null;
        File file = null;
        switch (def) {
            case TXT:
                ret = createTextReport(_parameter);
                break;
            case XLS:
                dyRp = getReport(_parameter);
                dyRp.setFileName(DBProperties.getProperty("org.efaps.esjp.payroll.reports.AFPFileName"));
                file = dyRp.getExcel(_parameter);
                ret.put(ReturnValues.VALUES, file);
                ret.put(ReturnValues.TRUE, true);
                break;
            case PDF:
                dyRp = getReport(_parameter);
                dyRp.setFileName(DBProperties.getProperty("org.efaps.esjp.payroll.reports.AFPFileName"));
                file = dyRp.getPDF(_parameter);
                ret.put(ReturnValues.VALUES, file);
                ret.put(ReturnValues.TRUE, true);
                break;
            default:
                break;
        }
        return ret;
    }

    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new RetentionCertificateReport();
    }

    public class RetentionCertificateReport
        extends AbstractDynamicReport
    {

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final DRDataSource dataSource = new DRDataSource("codCuspp", "docNum", "lastName", "lastName2", "name",
                            "movType", "movDate", "remuI", "remuJ", "remuK", "remuL", "rubRisk", "afp");

            final DateTime dateFrom = new DateTime(_parameter
                            .getParameterValue(CIFormPayroll.Payroll_ExportAFPReportForm.dateFrom.name));
            final DateTime dateTo = new DateTime(_parameter
                            .getParameterValue(CIFormPayroll.Payroll_ExportAFPReportForm.dateTo.name));

            final SystemConfiguration config = Payroll.getSysConfig();
            final String movTypeEndDateOids = config.getAttributeValue(PayrollSettings.MOVEMENTTYPE_ENDDATE);
            final Set<String> setMovTypeEndDate = new HashSet<String>();
            if (movTypeEndDateOids != null && !movTypeEndDateOids.isEmpty()) {
                final String[] movTypeEndDateStr = movTypeEndDateOids.split(";");
                for (final String movTypeOid : movTypeEndDateStr) {
                    final Instance instMovType = Instance.get(movTypeOid);
                    if (instMovType.isValid()) {
                        setMovTypeEndDate.add(instMovType.getOid());
                    }
                }
            }

            final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.Payslip);
            attrQueryBldr.addWhereAttrGreaterValue(CIPayroll.Payslip.Date, dateFrom.minusMinutes(1));
            attrQueryBldr.addWhereAttrLessValue(CIPayroll.Payslip.DueDate, dateTo.plusDays(1));
            final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIPayroll.Payslip.ID);

            final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.Payslip);
            queryBldr.addWhereAttrInQuery(CIPayroll.Payslip.ID, attrQuery);

            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIPayroll.Payslip.Date,
                                CIPayroll.Payslip.DueDate);
            final SelectBuilder selDoc = new SelectBuilder()
                            .linkto(CIPayroll.Payslip.EmployeeAbstractLink).attribute(CIHumanResource.Employee.Number);
            final SelectBuilder selEmpLName = new SelectBuilder()
                            .linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                            .attribute(CIHumanResource.Employee.LastName);
            final SelectBuilder selEmpSLName = new SelectBuilder()
                            .linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                            .attribute(CIHumanResource.Employee.SecondLastName);
            final SelectBuilder selEmpFName = new SelectBuilder()
                            .linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                            .attribute(CIHumanResource.Employee.FirstName);
            final SelectBuilder selCuspp = new SelectBuilder()
                            .linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                            .clazz(CIHumanResource.ClassTR_Health)
                            .attribute(CIHumanResource.ClassTR_Health.CUSPP);
            final SelectBuilder selRegime = new SelectBuilder()
                            .linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                            .clazz(CIHumanResource.ClassTR_Health)
                            .linkto(CIHumanResource.ClassTR_Health.HealthRegimeLink)
                            .attribute(CIHumanResource.AttributeDefinitionHealthRegime.Value);
            final SelectBuilder selMovTypeInst = new SelectBuilder()
                            .linkto(CIPayroll.Payslip.MovementType).instance();
            final SelectBuilder selMovType = new SelectBuilder()
                            .linkto(CIPayroll.Payslip.MovementType)
                            .attribute(CIPayroll.AttributeDefinitionMovementType.Value);
            multi.addSelect(selDoc, selEmpLName, selEmpFName, selEmpSLName,
                            selCuspp, selRegime, selMovTypeInst, selMovType);
            multi.execute();
            final List<Map<String, Object>> lst = new ArrayList<Map<String, Object>>();
            while (multi.next()) {
                final Map<String, Object> map = new HashMap<String, Object>();
                final String doc = multi.<String>getSelect(selDoc);
                final String lName = multi.<String>getSelect(selEmpLName);
                final String slName = multi.<String>getSelect(selEmpSLName);
                final String fName = multi.<String>getSelect(selEmpFName);
                final String security = multi.<String>getSelect(selRegime);
                final String securityNumb = multi.<String>getSelect(selCuspp);
                final Instance movTypeInst = multi.<Instance>getSelect(selMovTypeInst);
                final String movType = multi.<String>getSelect(selMovType);
                DateTime movDate = multi.<DateTime>getAttribute(CIPayroll.Payslip.Date);
                if (setMovTypeEndDate.contains(movTypeInst.getOid())) {
                    movDate = multi.<DateTime>getAttribute(CIPayroll.Payslip.DueDate);
                }
                map.put("docNum", doc);
                map.put("lastName", lName);
                map.put("lastName2", slName);
                map.put("name", fName);
                map.put("codCuspp", securityNumb);
                map.put("afp", security);
                map.put("movType", movType);
                map.put("movDate", movDate.toDate());

                final QueryBuilder queryBldr2 = new QueryBuilder(CIPayroll.PositionAbstract);
                queryBldr2.addWhereAttrNotEqValue(CIPayroll.PositionAbstract.Type,
                                CIPayroll.PositionSum.getType().getId());
                queryBldr2.addWhereAttrEqValue(CIPayroll.PositionAbstract.DocumentAbstractLink,
                                multi.getCurrentInstance().getId());
                final MultiPrintQuery multi2 = queryBldr2.getPrint();
                multi2.addAttribute(CIPayroll.PositionAbstract.Amount);
                final SelectBuilder selCaseExp = new SelectBuilder()
                                .linkto(CIPayroll.PositionAbstract.CasePositionAbstractLink)
                                .attribute(CIPayroll.CasePositionCalc.ExportAFP);
                multi2.addSelect(selCaseExp);
                multi2.execute();
                while (multi2.next()) {
                    final BigDecimal amount = multi2.<BigDecimal>getAttribute(CIPayroll.PositionAbstract.Amount);
                    final String caseExpAfp = multi2.<String>getSelect(selCaseExp);
                    if (caseExpAfp != null && !caseExpAfp.isEmpty()) {
                        map.put(caseExpAfp, amount);
                    }
                }
                lst.add(map);
            }
            for (final Map<String, Object> map : lst) {
                dataSource.add(map.get("codCuspp") != null ? map.get("codCuspp") : "",
                                map.get("docNum"),
                                map.get("lastName"),
                                map.get("lastName2"),
                                map.get("name"),
                                map.get("movType"),
                                map.get("movDate"),
                                map.get("I"),
                                map.get("J"),
                                map.get("K"),
                                map.get("L"),
                                "",
                                map.get("afp") != null ? map.get("afp") : "");
            }
            return dataSource;
        }

        @Override
        protected void addColumnDefintion(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            _builder.addColumn(
                            DynamicReports.col.reportRowNumberColumn(),
                            DynamicReports.col.column(DBProperties
                                            .getProperty("org.efaps.esjp.payroll.reports.ExportAFPReport.CodCuspp"),
                                            "codCuspp", DynamicReports.type.stringType()),
                            DynamicReports.col.column(DBProperties
                                            .getProperty("org.efaps.esjp.payroll.reports.ExportAFPReport.DocNum"),
                                            "docNum", DynamicReports.type.stringType()),
                            DynamicReports.col.column(DBProperties
                                            .getProperty("org.efaps.esjp.payroll.reports.ExportAFPReport.LastName"),
                                            "lastName", DynamicReports.type.stringType()),
                            DynamicReports.col.column(DBProperties
                                            .getProperty("org.efaps.esjp.payroll.reports.ExportAFPReport.LastName2"),
                                            "lastName2", DynamicReports.type.stringType()),
                            DynamicReports.col.column(DBProperties
                                            .getProperty("org.efaps.esjp.payroll.reports.ExportAFPReport.Name"),
                                            "name", DynamicReports.type.stringType()),
                            DynamicReports.col.column(DBProperties
                                            .getProperty("org.efaps.esjp.payroll.reports.ExportAFPReport.MovType"),
                                            "movType", DynamicReports.type.stringType()),
                            DynamicReports.col.column(DBProperties
                                            .getProperty("org.efaps.esjp.payroll.reports.ExportAFPReport.MovDate"),
                                            "movDate", DynamicReports.type.dateType()),
                            DynamicReports.col.column(DBProperties
                                            .getProperty("org.efaps.esjp.payroll.reports.ExportAFPReport.RemuI"),
                                            "remuI", DynamicReports.type.bigDecimalType()),
                            DynamicReports.col.column(DBProperties
                                            .getProperty("org.efaps.esjp.payroll.reports.ExportAFPReport.RemuJ"),
                                            "remuJ", DynamicReports.type.bigDecimalType()),
                            DynamicReports.col.column(DBProperties
                                            .getProperty("org.efaps.esjp.payroll.reports.ExportAFPReport.RemuK"),
                                            "remuK", DynamicReports.type.bigDecimalType()),
                            DynamicReports.col.column(DBProperties
                                            .getProperty("org.efaps.esjp.payroll.reports.ExportAFPReport.RemuL"),
                                            "remuL", DynamicReports.type.bigDecimalType()),
                            DynamicReports.col.column(DBProperties
                                            .getProperty("org.efaps.esjp.payroll.reports.ExportAFPReport.RubRisk"),
                                            "rubRisk", DynamicReports.type.stringType()),
                            DynamicReports.col.column(DBProperties
                                            .getProperty("org.efaps.esjp.payroll.reports.ExportAFPReport.Afp"),
                                            "afp", DynamicReports.type.stringType())
                            );
        }
    }

    public Return createTextReport(final Parameter _parameter)
        throws EFapsException
    {
        return new EFapsTextReport()
        {
            @Override
            protected File getEmptyFile4TextReport()
                throws EFapsException
            {
                final String name = buildName4TextReport(DBProperties
                                .getProperty("org.efaps.esjp.payroll.reports.AFPFileName"));
                final File file = new FileUtil().getFile(name, "txt");
                return file;
            }

            @Override
            protected void addHeaderDefinition(final Parameter _parameter,
                                               final List<ColumnDefinition> _columnsList)
                throws EFapsException
            {
            }

            @Override
            protected List<Object> getHeaderData(final Parameter _parameter)
                throws EFapsException
            {
                return null;
            }

            @Override
            protected void addColumnDefinition(final Parameter _parameter,
                                               final List<ColumnDefinition> _columnsList)
                throws EFapsException
            {
                // without '-' is align to right
                final DecimalFormat integerFormat = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext()
                                .getLocale());
                integerFormat.setMaximumIntegerDigits(5);
                integerFormat.setMinimumIntegerDigits(5);
                integerFormat.setGroupingUsed(false);
                _columnsList.add(new ColumnDefinition("", 5, 0, null, integerFormat, "0", Type.NUMBERTYPE, null));
                // with '-' is align to left
                _columnsList.add(new ColumnDefinition("", 12, 0, null, null, " ", Type.STRINGTYPE, "%1$-12s"));
                _columnsList.add(new ColumnDefinition("", 10, 0, null, null, " ", Type.STRINGTYPE, "%1$-10s"));
                _columnsList.add(new ColumnDefinition("", 20, 0, null, null, " ", Type.STRINGTYPE, "%1$-20s"));
                _columnsList.add(new ColumnDefinition("", 20, 0, null, null, " ", Type.STRINGTYPE, "%1$-20s"));
                _columnsList.add(new ColumnDefinition("", 20, 0, null, null, " ", Type.STRINGTYPE, "%1$-20s"));
                _columnsList.add(new ColumnDefinition("", 1, 0, null, null, " ", Type.STRINGTYPE, "%1$-1s"));
                _columnsList.add(new ColumnDefinition("", 10, 0, null, new SimpleDateFormat("dd/MM/yyyy"),
                                " ", Type.DATETYPE, "%1$-10s"));

                final DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext()
                                .getLocale());
                decimalFormat.setMaximumIntegerDigits(7);
                decimalFormat.setMaximumFractionDigits(2);
                decimalFormat.setMinimumIntegerDigits(7);
                decimalFormat.setMinimumFractionDigits(2);
                decimalFormat.setGroupingUsed(false);
                decimalFormat.setRoundingMode(RoundingMode.HALF_UP);
                _columnsList.add(new ColumnDefinition("", 7, 2, true, decimalFormat, "0", Type.NUMBERTYPE, null));
                _columnsList.add(new ColumnDefinition("", 7, 2, true, decimalFormat, "0", Type.NUMBERTYPE, null));
                _columnsList.add(new ColumnDefinition("", 7, 2, true, decimalFormat, "0", Type.NUMBERTYPE, null));
                _columnsList.add(new ColumnDefinition("", 7, 2, true, decimalFormat, "0", Type.NUMBERTYPE, null));
                _columnsList.add(new ColumnDefinition("", 1, 0, null, null, " ", Type.STRINGTYPE, "%1$-1s"));
                _columnsList.add(new ColumnDefinition("", 2, 0, null, null, " ", Type.STRINGTYPE, "%1$-2s"));
            }

            @Override
            protected List<List<Object>> createDataSource(final Parameter _parameter)
                throws EFapsException
            {
                final List<List<Object>> lst = new ArrayList<List<Object>>();

                final SystemConfiguration config = Payroll.getSysConfig();
                final String movTypeEndDateOids = config.getAttributeValue(PayrollSettings.MOVEMENTTYPE_ENDDATE);
                final Set<String> setMovTypeEndDate = new HashSet<String>();
                if (movTypeEndDateOids != null && !movTypeEndDateOids.isEmpty()) {
                    final String[] movTypeEndDateStr = movTypeEndDateOids.split(";");
                    for (final String movTypeOid : movTypeEndDateStr) {
                        final Instance instMovType = Instance.get(movTypeOid);
                        if (instMovType.isValid()) {
                            setMovTypeEndDate.add(instMovType.getOid());
                        }
                    }
                }

                final DateTime dateFrom = new DateTime(_parameter
                                .getParameterValue(CIFormPayroll.Payroll_ExportAFPReportForm.dateFrom.name));
                final DateTime dateTo = new DateTime(_parameter
                                .getParameterValue(CIFormPayroll.Payroll_ExportAFPReportForm.dateTo.name));

                final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.Payslip);
                attrQueryBldr.addWhereAttrGreaterValue(CIPayroll.Payslip.Date, dateFrom.minusMinutes(1));
                attrQueryBldr.addWhereAttrLessValue(CIPayroll.Payslip.DueDate, dateTo.plusDays(1));
                final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIPayroll.Payslip.ID);

                final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.Payslip);
                queryBldr.addWhereAttrInQuery(CIPayroll.Payslip.ID, attrQuery);

                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CIPayroll.Payslip.Date,
                                CIPayroll.Payslip.DueDate);
                final SelectBuilder selDoc = new SelectBuilder()
                                .linkto(CIPayroll.Payslip.EmployeeAbstractLink).attribute(CIHumanResource.Employee.Number);
                final SelectBuilder selEmpLName = new SelectBuilder()
                                .linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                                .attribute(CIHumanResource.Employee.LastName);
                final SelectBuilder selEmpSLName = new SelectBuilder()
                                .linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                                .attribute(CIHumanResource.Employee.SecondLastName);
                final SelectBuilder selEmpFName = new SelectBuilder()
                                .linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                                .attribute(CIHumanResource.Employee.FirstName);
                final SelectBuilder selCuspp = new SelectBuilder()
                                .linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                                .clazz(CIHumanResource.ClassTR_Health)
                                .attribute(CIHumanResource.ClassTR_Health.CUSPP);
                final SelectBuilder selRegime = new SelectBuilder()
                                .linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                                .clazz(CIHumanResource.ClassTR_Health)
                                .linkto(CIHumanResource.ClassTR_Health.HealthRegimeLink)
                                .attribute(CIHumanResource.AttributeDefinitionHealthRegime.Value);
                final SelectBuilder selMovTypeInst = new SelectBuilder()
                                .linkto(CIPayroll.Payslip.MovementType).instance();
                final SelectBuilder selMovType = new SelectBuilder()
                                .linkto(CIPayroll.Payslip.MovementType)
                                .attribute(CIPayroll.AttributeDefinitionMovementType.Value);
                multi.addSelect(selDoc, selEmpLName, selEmpFName, selEmpSLName,
                                selCuspp, selRegime, selMovType, selMovTypeInst);
                multi.execute();
                final List<Map<String, Object>> lstMap = new ArrayList<Map<String, Object>>();
                while (multi.next()) {
                    final Map<String, Object> map = new HashMap<String, Object>();
                    final String doc = multi.<String>getSelect(selDoc);
                    final String lName = multi.<String>getSelect(selEmpLName);
                    final String slName = multi.<String>getSelect(selEmpSLName);
                    final String fName = multi.<String>getSelect(selEmpFName);
                    final String security = multi.<String>getSelect(selRegime);
                    final String securityNumb = multi.<String>getSelect(selCuspp);
                    final Instance movTypeInst = multi.<Instance>getSelect(selMovTypeInst);
                    final String movType = multi.<String>getSelect(selMovType);
                    DateTime movDate = multi.<DateTime>getAttribute(CIPayroll.Payslip.Date);
                    if (setMovTypeEndDate.contains(movTypeInst.getOid())) {
                        movDate = multi.<DateTime>getAttribute(CIPayroll.Payslip.DueDate);
                    }
                    map.put("docNum", doc);
                    map.put("lastName", lName);
                    map.put("lastName2", slName);
                    map.put("name", fName);
                    map.put("codCuspp", securityNumb);
                    map.put("afp", security);
                    map.put("movType", movType);
                    map.put("movDate", movDate);

                    final QueryBuilder queryBldr2 = new QueryBuilder(CIPayroll.PositionAbstract);
                    queryBldr2.addWhereAttrNotEqValue(CIPayroll.PositionAbstract.Type,
                                    CIPayroll.PositionSum.getType().getId());
                    queryBldr2.addWhereAttrEqValue(CIPayroll.PositionAbstract.DocumentAbstractLink,
                                    multi.getCurrentInstance().getId());
                    final MultiPrintQuery multi2 = queryBldr2.getPrint();
                    multi2.addAttribute(CIPayroll.PositionAbstract.Amount);
                    final SelectBuilder selCaseExp = new SelectBuilder()
                                    .linkto(CIPayroll.PositionAbstract.CasePositionAbstractLink)
                                    .attribute(CIPayroll.CasePositionCalc.ExportAFP);
                    multi2.addSelect(selCaseExp);
                    multi2.execute();
                    while (multi2.next()) {
                        final BigDecimal amount = multi2.<BigDecimal>getAttribute(CIPayroll.PositionAbstract.Amount);
                        final String caseExpAfp = multi2.<String>getSelect(selCaseExp);
                        if (caseExpAfp != null && !caseExpAfp.isEmpty()) {
                            map.put(caseExpAfp, amount);
                        }
                    }
                    lstMap.add(map);
                }
                Integer cont = 1;
                for (final Map<String, Object> map : lstMap) {
                    final List<Object> rowLst = new ArrayList<Object>();
                    rowLst.add(cont);
                    rowLst.add(map.get("codCuspp") != null ? map.get("codCuspp") : "");
                    rowLst.add(map.get("docNum"));
                    rowLst.add(map.get("lastName"));
                    rowLst.add(map.get("lastName2"));
                    rowLst.add(map.get("name"));
                    rowLst.add(map.get("movType"));
                    rowLst.add(map.get("movDate"));
                    rowLst.add(map.get("I"));
                    rowLst.add(map.get("J"));
                    rowLst.add(map.get("K"));
                    rowLst.add(map.get("L"));
                    rowLst.add("");
                    rowLst.add(map.get("afp") != null ? map.get("afp") : "");
                    cont++;
                    lst.add(rowLst);
                }

                return lst;
            }
        }.getTextReport(_parameter);
    }
}
