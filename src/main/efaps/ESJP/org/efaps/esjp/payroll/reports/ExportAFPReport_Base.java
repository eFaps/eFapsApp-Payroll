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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIFormPayroll;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.payroll.util.Payroll;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

/**
 * Report to be used to import in the Website provided by the AFP.
 *
 * @author The eFaps Team
 */
@EFapsUUID("bb288739-7ecc-498b-9633-f32f21978999")
@EFapsApplication("eFapsApp-Payroll")
public abstract class ExportAFPReport_Base
    extends AbstractReports
{

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Return containing the report
     * @throws EFapsException on error
     */
    public Return createReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();

        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName(DBProperties.getProperty("org.efaps.esjp.payroll.reports.AFPFileName"));

        ret.put(ReturnValues.VALUES, dyRp.getExcel(_parameter));
        ret.put(ReturnValues.TRUE, true);
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Dycnamic Report
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynExportAFPReport();
    }

    /**
     * Report class.
     */
    public static class DynExportAFPReport
        extends AbstractDynamicReport
    {

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final List<DataBean> beans = new ArrayList<>();

            final String totalKey = Payroll.RULE4AFPTOTAL.get();
            if (totalKey != null && !totalKey.isEmpty()) {
                final DateTime dateFrom = new DateTime(_parameter
                                .getParameterValue(CIFormPayroll.Payroll_ExportAFPReportForm.dateFrom.name));
                final DateTime dateTo = new DateTime(_parameter
                                .getParameterValue(CIFormPayroll.Payroll_ExportAFPReportForm.dateTo.name));

                final QueryBuilder pensAttrQueryBldr = new QueryBuilder(CIHumanResource.ClassTR_Pensioner);

                final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.Payslip);
                attrQueryBldr.addWhereAttrNotInQuery(CIPayroll.Payslip.EmployeeAbstractLink,
                                pensAttrQueryBldr.getAttributeQuery(CIHumanResource.ClassTR_Pensioner.EmployeeLink));
                attrQueryBldr.addWhereAttrGreaterValue(CIPayroll.Payslip.Date, dateFrom.minusMinutes(1));
                attrQueryBldr.addWhereAttrLessValue(CIPayroll.Payslip.DueDate, dateTo.plusDays(1));

                final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.PositionAbstract);
                queryBldr.addWhereAttrInQuery(CIPayroll.PositionAbstract.DocumentAbstractLink,
                                attrQueryBldr.getAttributeQuery(CIPayroll.Payslip.ID));
                queryBldr.addWhereAttrEqValue(CIPayroll.PositionAbstract.Key, totalKey);

                final MultiPrintQuery multi = queryBldr.getPrint();

                final SelectBuilder selEmpl = SelectBuilder.get()
                                .linkto(CIPayroll.PositionAbstract.DocumentAbstractLink)
                                .linkto(CIPayroll.Payslip.EmployeeAbstractLink);

                final SelectBuilder selEmplInst = new SelectBuilder(selEmpl).instance();
                final SelectBuilder selCuspp = new SelectBuilder(selEmpl).clazz(CIHumanResource.ClassTR_Health)
                                .attribute(CIHumanResource.ClassTR_Health.CUSPP);
                final SelectBuilder selEmplDocType = new SelectBuilder(selEmpl)
                                .linkto(CIHumanResource.Employee.NumberTypeLink)
                                .attribute(CIERP.AttributeDefinitionAbstract.MappingKey);
                final SelectBuilder selEmplNumber = new SelectBuilder(selEmpl)
                                .attribute(CIHumanResource.Employee.Number);
                final SelectBuilder selEmplLastName = new SelectBuilder(selEmpl)
                                .attribute(CIHumanResource.Employee.LastName);
                final SelectBuilder selEmplSecondLastName = new SelectBuilder(selEmpl)
                                .attribute(CIHumanResource.Employee.SecondLastName);
                final SelectBuilder selEmplName = new SelectBuilder(selEmpl)
                                .attribute(CIHumanResource.Employee.FirstName);
                final SelectBuilder selStartDate = new SelectBuilder(selEmpl).clazz(CIHumanResource.ClassTR)
                                .attribute(CIHumanResource.ClassTR.StartDate);
                multi.addAttribute(CIPayroll.PositionAbstract.Amount);
                multi.addSelect(selEmplInst, selEmplDocType, selEmplNumber, selEmplLastName, selEmplSecondLastName,
                                selCuspp, selEmplName, selStartDate);
                multi.execute();
                final Map<Instance, DataBean> map = new HashMap<>();
                while (multi.next()) {
                    final String cuspp = multi.<String>getSelect(selCuspp);
                    if (cuspp != null && !cuspp.isEmpty()) {
                        final Instance emplInst = multi.getSelect(selEmplInst);
                        final DataBean bean;
                        if (map.containsKey(emplInst)) {
                            bean = map.get(emplInst);
                        } else {
                            bean = new DataBean().setCuspp(cuspp)
                                            .evaluateEmplDocType(multi.<String>getSelect(selEmplDocType))
                                            .setEmplNumber(multi.<String>getSelect(selEmplNumber))
                                            .setEmplLastName(multi.<String>getSelect(selEmplLastName))
                                            .setEmplSecondLastName(multi.<String>getSelect(selEmplSecondLastName))
                                            .setEmplName(multi.<String>getSelect(selEmplName));
                            map.put(emplInst, bean);
                        }
                        bean.setRemuneration(bean.getRemuneration().add(
                                        multi.<BigDecimal>getAttribute(CIPayroll.PositionAbstract.Amount)));

                        bean.setWorkRel(true);

                        final DateTime startdate = multi.getSelect(selStartDate);
                        if (startdate.isAfter(dateFrom.minusMinutes(1)) && startdate.isBefore(dateTo.plusDays(1))) {
                            bean.setWorkRelStart(true);
                        }
                    }
                }
                beans.addAll(map.values());
            }

            Collections.sort(beans, new Comparator<DataBean>()
            {

                @Override
                public int compare(final DataBean _arg0,
                                   final DataBean _arg1)
                {
                    return _arg0.getEmplNumber().compareTo(_arg1.getEmplNumber());
                }
            });
            return new JRBeanCollectionDataSource(beans);
        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                          final JasperReportBuilder _builder)
            throws EFapsException
        {
            _builder.addColumn(
                            DynamicReports.col.reportRowNumberColumn(), //A
                            DynamicReports.col.column("cuspp", DynamicReports.type.stringType()), //B
                            DynamicReports.col.column("emplDocType", DynamicReports.type.integerType()), //C
                            DynamicReports.col.column("emplNumber", DynamicReports.type.stringType()), //D
                            DynamicReports.col.column("emplLastName", DynamicReports.type.stringType()), //E
                            DynamicReports.col.column("emplSecondLastName", DynamicReports.type.stringType()), //F
                            DynamicReports.col.column("emplName", DynamicReports.type.stringType()),//G
                            DynamicReports.col.column("workRel", DynamicReports.type.stringType()), //H
                            DynamicReports.col.column("workRelStart", DynamicReports.type.stringType()), //I
                            DynamicReports.col.column("workRelStop", DynamicReports.type.stringType()), //J
                            DynamicReports.col.column("contributeException", DynamicReports.type.stringType()), //K
                            DynamicReports.col.column("remuneration", DynamicReports.type.bigDecimalType()), //L
                            DynamicReports.col.column("voluntary1", DynamicReports.type.bigDecimalType()), //M
                            DynamicReports.col.column("voluntary2", DynamicReports.type.bigDecimalType()), //N
                            DynamicReports.col.column("voluntary3", DynamicReports.type.bigDecimalType()), //O
                            DynamicReports.col.column("emplType", DynamicReports.type.stringType()),//P
                            DynamicReports.col.column("afp", DynamicReports.type.stringType())//Q
                            );
        }
    }

    public static class DataBean
    {

        private String cuspp;
        private String emplNumber;
        private Integer emplDocType;
        private String emplLastName;
        private String emplSecondLastName;
        private String emplName;
        private boolean workRel;
        private boolean workRelStart;
        private boolean workRelStop;
        private String contributeException;
        private BigDecimal remuneration = BigDecimal.ZERO;
        private BigDecimal voluntary1 = BigDecimal.ZERO;
        private BigDecimal voluntary2 = BigDecimal.ZERO;
        private BigDecimal voluntary3 = BigDecimal.ZERO;
        private String emplType;
        private String afp;

        public String getWorkRel()
        {
            return this.workRel ? "S" : "N";
        }

        public DataBean setWorkRel(final boolean  _hasWorkRel)
        {
            this.workRel = _hasWorkRel;
            return this;
        }

        public String getWorkRelStart()
        {
            return this.workRelStart ? "S" : "N";
        }

        public DataBean setWorkRelStart(final boolean  _workRelStart)
        {
            this.workRelStart = _workRelStart;
            return this;
        }

        public String getWorkRelStop()
        {
            return this.workRelStop ? "S" : "N";
        }

        public DataBean setWorkRelStop(final boolean _workRelStop)
        {
            this.workRelStop = _workRelStop;
            return this;
        }

        public String getContributeException()
        {
            return this.contributeException;
        }

        public DataBean setContributeException(final String _contributeException)
        {
            this.contributeException = _contributeException;
            return this;
        }

        /**
         * @param _select
         * @return this for chaining
         */
        public DataBean evaluateEmplDocType(final String _select)
        {
            switch (_select) {
                case "01":
                    // DNI
                    setEmplDocType(0);
                    break;
                case "04":
                    // CE
                    setEmplDocType(1);
                    break;
                case "06":
                    // RUC
                    break;
                case "07":
                    // PASAPORTE
                    setEmplDocType(4);
                    break;
                case "11":
                    // PARTIDA DE NACIMIENTO
                    break;
                default:
                    break;
            }
            return this;
        }

        /**
         * Getter method for the instance variable {@link #cuspp}.
         *
         * @return value of instance variable {@link #cuspp}
         */
        public String getCuspp()
        {
            return this.cuspp;
        }

        /**
         * Setter method for instance variable {@link #cuspp}.
         *
         * @param _cuspp value for instance variable {@link #cuspp}
         * @return this for chaining
         */
        public DataBean setCuspp(final String _cuspp)
        {
            this.cuspp = _cuspp;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #emplNumber}.
         *
         * @return value of instance variable {@link #emplNumber}
         */
        public String getEmplNumber()
        {
            return this.emplNumber;
        }

        /**
         * Setter method for instance variable {@link #emplNumber}.
         *
         * @param _emplNumber value for instance variable {@link #emplNumber}
         * @return this for chaining
         */
        public DataBean setEmplNumber(final String _emplNumber)
        {
            this.emplNumber = _emplNumber;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #emplDocType}.
         *
         * @return value of instance variable {@link #emplDocType}
         */
        public Integer getEmplDocType()
        {
            return this.emplDocType;
        }

        /**
         * Setter method for instance variable {@link #emplDocType}.
         *
         * @param _emplDocType value for instance variable {@link #emplDocType}
         * @return this for chaining
         */
        public DataBean setEmplDocType(final Integer _emplDocType)
        {
            this.emplDocType = _emplDocType;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #emplLastName}.
         *
         * @return value of instance variable {@link #emplLastName}
         */
        public String getEmplLastName()
        {
            return this.emplLastName;
        }

        /**
         * Setter method for instance variable {@link #emplLastName}.
         *
         * @param _emplLastName value for instance variable
         *            {@link #emplLastName}
         * @return this for chaining
         */
        public DataBean setEmplLastName(final String _emplLastName)
        {
            this.emplLastName = _emplLastName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #emplSecondLastName}.
         *
         * @return value of instance variable {@link #emplSecondLastName}
         */
        public String getEmplSecondLastName()
        {
            return this.emplSecondLastName;
        }

        /**
         * Setter method for instance variable {@link #emplSecondLastName}.
         *
         * @param _emplSecondLastName value for instance variable
         *            {@link #emplSecondLastName}
         * @return this for chaining
         */
        public DataBean setEmplSecondLastName(final String _emplSecondLastName)
        {
            this.emplSecondLastName = _emplSecondLastName;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #emplName}.
         *
         * @return value of instance variable {@link #emplName}
         */
        public String getEmplName()
        {
            return this.emplName;
        }

        /**
         * Setter method for instance variable {@link #emplName}.
         *
         * @param _emplName value for instance variable {@link #emplName}
         * @return this for chaining
         */
        public DataBean setEmplName(final String _emplName)
        {
            this.emplName = _emplName;
            return this;
        }



        /**
         * Getter method for the instance variable {@link #remuneration}.
         *
         * @return value of instance variable {@link #remuneration}
         */
        public BigDecimal getRemuneration()
        {
            // TODO

            return this.remuneration;
        }

        /**
         * Getter method for the instance variable {@link #voluntary1}.
         *
         * @return value of instance variable {@link #voluntary1}
         */
        public BigDecimal getVoluntary1()
        {
            return this.voluntary1;
        }

        /**
         * Setter method for instance variable {@link #voluntary1}.
         *
         * @param _voluntary1 value for instance variable {@link #voluntary1}
         * @return this for chaining
         */
        public DataBean setVoluntary1(final BigDecimal _voluntary1)
        {
            this.voluntary1 = _voluntary1;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #voluntary2}.
         *
         * @return value of instance variable {@link #voluntary2}
         */
        public BigDecimal getVoluntary2()
        {
            return this.voluntary2;
        }

        /**
         * Setter method for instance variable {@link #voluntary2}.
         *
         * @param _voluntary2 value for instance variable {@link #voluntary2}
         * @return this for chaining
         */
        public DataBean setVoluntary2(final BigDecimal _voluntary2)
        {
            this.voluntary2 = _voluntary2;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #voluntary3}.
         *
         * @return value of instance variable {@link #voluntary3}
         */
        public BigDecimal getVoluntary3()
        {
            return this.voluntary3;
        }

        /**
         * Setter method for instance variable {@link #voluntary3}.
         *
         * @param _voluntary3 value for instance variable {@link #voluntary3}
         * @return this for chaining
         */
        public DataBean setVoluntary3(final BigDecimal _voluntary3)
        {
            this.voluntary3 = _voluntary3;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #emplType}.
         *
         * @return value of instance variable {@link #emplType}
         */
        public String getEmplType()
        {
            return this.emplType;
        }

        /**
         * Setter method for instance variable {@link #emplType}.
         *
         * @param _emplType value for instance variable {@link #emplType}
         * @return this for chaining
         */
        public DataBean setEmplType(final String _emplType)
        {
            this.emplType = _emplType;
            return this;
        }

        /**
         * Getter method for the instance variable {@link #afp}.
         *
         * @return value of instance variable {@link #afp}
         */
        public String getAfp()
        {
            return this.afp;
        }

        /**
         * Setter method for instance variable {@link #afp}.
         *
         * @param _afp value for instance variable {@link #afp}
         * @return this for chaining
         */
        public DataBean setAfp(final String _afp)
        {
            this.afp = _afp;
            return this;
        }

        /**
         * Setter method for instance variable {@link #remuneration}.
         *
         * @param _remuneration value for instance variable
         *            {@link #remuneration}
         */
        public void setRemuneration(final BigDecimal _remuneration)
        {
            this.remuneration = _remuneration;
        }

    }

}
