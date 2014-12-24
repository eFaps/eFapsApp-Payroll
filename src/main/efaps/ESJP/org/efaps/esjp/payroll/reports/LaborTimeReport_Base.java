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
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: WorkOrderCalibrateDataSource.java 268 2011-04-29 17:10:40Z Jorge Cueva $
 */
@EFapsUUID("551faac7-0ac6-4794-bdd7-44839a746ad5")
@EFapsRevision("$Rev: 295 $")
public abstract class LaborTimeReport_Base
    extends AbstractReports
{

    private static String format = "0601";

    /**
     * Enum used to define the keys for the map.
     */
    public enum Field implements Column
    {
        ID("", 6, null),
        /** */
        DOCTYPE("docType", 2, null),
        /** */
        DOCNUM("docNum", 15, null),
        /** */
        LABTIMEHOUR("laborTime_Hour", 3, null),
        /** */
        LABTIMEMIN("laborTime_Min", 2, null),
        /** */
        LABEXTIMEHOUR("laborExtraTime_Hour", 3, null),
        /** */
        LABEXTIMEMIN("laborExtraTime_Min", 2, null);

        /**
         * key.
         */
        private final String key;

        /**
         * length.
         */
        private final Integer lenght;

        /**
         * decimalLength.
         */
        private final Integer decimalLength;

        /**
         * @param _key key
         */
        private Field(final String _key,
                      final Integer _length,
                      final Integer _decimalLength)
        {
            this.key = _key;
            this.lenght = _length;
            this.decimalLength = _decimalLength;
        }

        /**
         * Getter method for the instance variable {@link #key}.
         *
         * @return value of instance variable {@link #key}
         */
        @Override
        public String getKey()
        {
            return this.key;
        }

        @Override
        public Integer getLength()
        {
            return this.lenght;
        }

        @Override
        public Integer getDecimalLength()
        {
            return this.decimalLength;
        }

        @Override
        public boolean isOptional()
        {
            return false;
        }

        @Override
        public String getDefaultVal()
        {
            return null;
        }

    }

    public Return execute(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final DateTime dateFrom = new DateTime(_parameter.getParameterValue("date"));

        final String name = getName4TextFile(LaborTimeReport_Base.format, dateFrom);

        File file;
        try {
            file = new FileUtil().getFile(name == null ? "JOR" : name, "jor");
            final PrintWriter writer = new PrintWriter(file);
            writer.print(getReportDataText(_parameter).toString());
            writer.close();
            ret.put(ReturnValues.VALUES, file);
            ret.put(ReturnValues.TRUE, true);
        } catch (final IOException e) {
            throw new EFapsException(LaborTimeReport_Base.class, "execute.IOException", e);
        }

        return ret;
    }

    protected String getReportDataText(final Parameter _parameter)
        throws EFapsException
    {
        final DateTime dateFrom = new DateTime(_parameter.getParameterValue("date"));
        final DateTime dateTo = new DateTime(_parameter.getParameterValue("dateTo"));
        final StringBuilder rep = new StringBuilder();
        final List<Map<String, Object>> values = getReportData(dateFrom, dateTo);
        boolean first = true;
        for (final Map<String, Object> map : values) {
            if (first) {
                first = false;
            } else {
                rep.append("\r\n");
            }
            rep.append(getCharacterValue(map.get(LaborTimeReport_Base.Field.DOCTYPE.getKey()),
                            LaborTimeReport_Base.Field.DOCTYPE)).append(getSeparator())
                .append(getCharacterValue(map.get(LaborTimeReport_Base.Field.DOCNUM.getKey()),
                                LaborTimeReport_Base.Field.DOCNUM)).append(getSeparator())
                .append(getNumberValue(map.get(LaborTimeReport_Base.Field.LABTIMEHOUR.getKey()),
                                LaborTimeReport_Base.Field.LABTIMEHOUR)).append(getSeparator())
                .append(getNumberValue(map.get(LaborTimeReport_Base.Field.LABTIMEMIN.getKey()),
                                LaborTimeReport_Base.Field.LABTIMEMIN)).append(getSeparator())
                .append(getNumberValue(map.get(LaborTimeReport_Base.Field.LABEXTIMEHOUR.getKey()),
                                LaborTimeReport_Base.Field.LABEXTIMEHOUR)).append(getSeparator())
                .append(getNumberValue(map.get(LaborTimeReport_Base.Field.LABEXTIMEMIN.getKey()),
                                            LaborTimeReport_Base.Field.LABEXTIMEMIN)).append(getSeparator());
        }
        AbstractReports_Base.LOG.debug(rep.toString());
        return rep.toString();
    }

    protected List<Map<String, Object>> getReportData(final DateTime _dateFrom,
                                                      final DateTime _dateTo)
        throws EFapsException
    {
        AbstractReports_Base.LOG.debug("dateFrom: '{}' dateTo: '{}'", _dateFrom, _dateTo);
        final List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();

        final QueryBuilder attrQueryBldr = new QueryBuilder(CIPayroll.Payslip);
        attrQueryBldr.addWhereAttrGreaterValue(CIPayroll.Payslip.Date, _dateFrom);
        attrQueryBldr.addWhereAttrLessValue(CIPayroll.Payslip.DueDate, _dateTo);
        final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIPayroll.Payslip.ID);

        final QueryBuilder attrQueryBldr2 = new QueryBuilder(CIPayroll.Payslip);
        attrQueryBldr2.addWhereAttrEqValue(CIPayroll.Payslip.Date, _dateFrom);
        attrQueryBldr2.addWhereAttrEqValue(CIPayroll.Payslip.DueDate, _dateTo);
        final AttributeQuery attrQuery2 = attrQueryBldr2.getAttributeQuery(CIPayroll.Payslip.ID);

        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.Payslip);
        queryBldr.addWhereAttrInQuery(CIPayroll.Payslip.ID, attrQuery);
        queryBldr.addWhereAttrInQuery(CIPayroll.Payslip.ID, attrQuery2);
        queryBldr.setOr(true);

        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIPayroll.Payslip.LaborTime, CIPayroll.Payslip.ExtraLaborTime);
        final SelectBuilder selDoc = new SelectBuilder()
                        .linkto(CIPayroll.Payslip.EmployeeAbstractLink).attribute(CIHumanResource.Employee.Number);
        final SelectBuilder selDocType = new SelectBuilder()
                        .linkto(CIPayroll.Payslip.EmployeeAbstractLink)
                        .linkto(CIHumanResource.Employee.NumberTypeLink)
                        .attribute(CIHumanResource.AttributeDefinitionDOIType.MappingKey);
        multi.addSelect(selDoc, selDocType);
        multi.execute();
        while (multi.next()) {
            final Map<String, Object> value = new HashMap<String, Object>();

            final String doc = multi.<String>getSelect(selDoc);
            final String docType = multi.<String>getSelect(selDocType);
            final Object[] laborTimeOb = multi.<Object[]>getAttribute(CIPayroll.Payslip.LaborTime);
            final Object[] extraLaborTimeOb = multi.<Object[]>getAttribute(CIPayroll.Payslip.ExtraLaborTime);

            final UoM laborTimeUom = (UoM) laborTimeOb[1];
            final BigDecimal laborTime = ((BigDecimal) laborTimeOb[0])
                            .multiply(new BigDecimal(laborTimeUom.getNumerator()))
                            .divide(new BigDecimal(laborTimeUom.getDenominator()), BigDecimal.ROUND_HALF_UP);
            final Integer laborTimeHour = laborTime.intValue();
            final Integer laborTimeMin = laborTime.subtract(new BigDecimal(laborTime.intValue()))
                            .multiply(new BigDecimal(60)).intValue();

            final UoM extraLaborTimeUom = (UoM) extraLaborTimeOb[1];
            final BigDecimal extraLaborTime = ((BigDecimal) extraLaborTimeOb[0])
                            .multiply(new BigDecimal(extraLaborTimeUom.getNumerator()))
                            .divide(new BigDecimal(extraLaborTimeUom.getDenominator()), BigDecimal.ROUND_HALF_UP);

            final Integer extraLaborTimeHour = extraLaborTime.intValue();
            final Integer extraLaborTimeMin = extraLaborTime.subtract(new BigDecimal(extraLaborTime.intValue()))
                            .multiply(new BigDecimal(60)).intValue();

            value.put(LaborTimeReport_Base.Field.DOCNUM.getKey(), doc);
            value.put(LaborTimeReport_Base.Field.DOCTYPE.getKey(), docType);
            value.put(LaborTimeReport_Base.Field.LABTIMEHOUR.getKey(), laborTimeHour);
            value.put(LaborTimeReport_Base.Field.LABTIMEMIN.getKey(), laborTimeMin);
            value.put(LaborTimeReport_Base.Field.LABEXTIMEHOUR.getKey(), extraLaborTimeHour);
            value.put(LaborTimeReport_Base.Field.LABEXTIMEMIN.getKey(), extraLaborTimeMin);
            values.add(value);

        }

        Collections.sort(values, new Comparator<Map<String, Object>>()
        {

            @Override
            public int compare(final Map<String, Object> _o1,
                               final Map<String, Object> _o2)
            {
                final String name1 = (String) _o1.get(LaborTimeReport_Base.Field.DOCNUM.getKey());
                final String name2 = (String) _o2.get(LaborTimeReport_Base.Field.DOCNUM.getKey());
                final int ret = name1.compareTo(name2);
                return ret;
            }
        });
        return values;
    }

}
