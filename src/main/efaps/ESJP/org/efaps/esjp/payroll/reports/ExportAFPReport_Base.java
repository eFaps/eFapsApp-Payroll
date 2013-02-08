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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.datasource.DRDataSource;
import net.sf.jasperreports.engine.JRDataSource;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIFormPayroll;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.uiform.Field;
import org.efaps.esjp.common.uiform.Field_Base.DropDownPosition;
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
    private static final List<DropDownPosition> REMUTYPE_LST = new ArrayList<DropDownPosition>();
    static {
        ExportAFPReport_Base.REMUTYPE_LST.add(new DropDownPosition("", ""));
        ExportAFPReport_Base.REMUTYPE_LST.add(new DropDownPosition("I", "Remuneración asegurable "));
        ExportAFPReport_Base.REMUTYPE_LST.add(new DropDownPosition("J", "Aporte voluntario con fin previsional "));
        ExportAFPReport_Base.REMUTYPE_LST.add(new DropDownPosition("K", "Aporte voluntario sin fin previsional "));
        ExportAFPReport_Base.REMUTYPE_LST.add(new DropDownPosition("L", "Aporte voluntario del empleador "));
    }

    public Return dropDownAfpRemuType(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final StringBuilder html = new Field().getDropDownField(_parameter, ExportAFPReport_Base.REMUTYPE_LST);
        ret.put(ReturnValues.SNIPLETT, html);
        return ret;
    }

    public Return createReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName("PlanillasAFP");
        final File file = dyRp.getExcel(_parameter);
        ret.put(ReturnValues.VALUES, file);
        ret.put(ReturnValues.TRUE, true);

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

            final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.Payslip);
            attrQueryBldr.addWhereAttrGreaterValue(CIPayroll.Payslip.Date, dateFrom);
            attrQueryBldr.addWhereAttrLessValue(CIPayroll.Payslip.DueDate, dateTo);
            final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIPayroll.Payslip.ID);

            final QueryBuilder attrQueryBldr2 = new QueryBuilder(CIPayroll.Payslip);
            attrQueryBldr2.addWhereAttrEqValue(CIPayroll.Payslip.Date, dateFrom);
            attrQueryBldr2.addWhereAttrEqValue(CIPayroll.Payslip.DueDate, dateTo);
            final AttributeQuery attrQuery2 = attrQueryBldr2.getAttributeQuery(CIPayroll.Payslip.ID);

            final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.Payslip);
            queryBldr.addWhereAttrInQuery(CIPayroll.Payslip.ID, attrQuery);
            queryBldr.addWhereAttrInQuery(CIPayroll.Payslip.ID, attrQuery2);
            queryBldr.setOr(true);

            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder selDoc = new SelectBuilder()
                            .linkto(CIPayroll.Payslip.EmployeeAbstractLink).attribute(CIHumanResource.Employee.Number);
            final SelectBuilder selEmpLName = new SelectBuilder()
                            .linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                            .attribute(CIHumanResource.Employee.LastName);
            final SelectBuilder selEmpFName = new SelectBuilder()
                            .linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                            .attribute(CIHumanResource.Employee.FirstName);
            final SelectBuilder selSecNumb = new SelectBuilder()
                            .linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                            .clazz(CIPayroll.HumanResource_EmployeeClassPayroll)
                            .attribute(CIPayroll.HumanResource_EmployeeClassPayroll.SecurityNumber);
            final SelectBuilder selSec = new SelectBuilder()
                            .linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                            .clazz(CIPayroll.HumanResource_EmployeeClassPayroll)
                            .linkto(CIPayroll.HumanResource_EmployeeClassPayroll.Security)
                            .attribute(CIPayroll.HumanResource_AttributeDefinitionSecurity.Value);
            multi.addSelect(selDoc, selEmpLName, selEmpFName, selSecNumb, selSec);
            multi.execute();
            final List<Map<String, Object>> lst = new ArrayList<Map<String, Object>>();
            while (multi.next()) {
                final Map<String, Object> map = new HashMap<String, Object>();
                final String doc = multi.<String>getSelect(selDoc);
                final String lName = multi.<String>getSelect(selEmpLName);
                final String fName = multi.<String>getSelect(selEmpFName);
                final String security = multi.<String>getSelect(selSec);
                final String securityNumb = multi.<String>getSelect(selSecNumb);
                map.put("docNum", doc);
                map.put("lastName", lName);
                map.put("name", fName);
                map.put("codCuspp", securityNumb);
                map.put("afp", security);

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
                                "",
                                map.get("name"),
                                "",
                                new DateTime().toDate(),
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

}
