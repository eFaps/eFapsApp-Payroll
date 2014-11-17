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

package org.efaps.esjp.payroll.rules;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.CachedPrintQuery;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIFormPayroll;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.ui.wicket.util.DateUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: AbstractParameter_Base.java 13971 2014-09-08 21:03:58Z
 *          jan@moxter.net $
 */
/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("45fcf09d-d676-4ac1-8f15-5f790f2eaf03")
@EFapsRevision("$Rev$")
public abstract class AbstractParameter_Base<T>
{

    protected static final String PARAKEY4EMPLOYINST = "EmployeeInstance";

    protected static final String PARAKEY4DATE = "Date";

    protected static final String PARAKEY4LT = "LaborTime";

    protected static final String PARAKEY4ELT = "ExtraLaborTime";

    protected static final String PARAKEY4NLT = "NightLaborTime";

    protected static final String PARAKEY4HLT = "HolidayLaborTime";


    /**
     * Instance of the rule.
     */
    private Instance instance;

    private String key;

    private Object value;

    private String description;

    protected abstract T getThis();

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
    public T setKey(final String _key)
    {
        this.key = _key;
        return getThis();
    }

    /**
     * Getter method for the instance variable {@link #value}.
     *
     * @return value of instance variable {@link #value}
     */
    public Object getValue()
        throws EFapsException
    {
        return this.value;
    }

    /**
     * Setter method for instance variable {@link #value}.
     *
     * @param _value value for instance variable {@link #value}
     */
    public T setValue(final Object _value)
    {
        this.value = _value;
        return getThis();
    }

    /**
     * Getter method for the instance variable {@link #description}.
     *
     * @return value of instance variable {@link #description}
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * Setter method for instance variable {@link #description}.
     *
     * @param _description value for instance variable {@link #description}
     */
    public T setDescription(final String _description)
    {
        this.description = _description;
        return getThis();
    }

    /**
     * Getter method for the instance variable {@link #instance}.
     *
     * @return value of instance variable {@link #instance}
     */
    public Instance getInstance()
    {
        return this.instance;
    }

    /**
     * Setter method for instance variable {@link #instance}.
     *
     * @param _instance value for instance variable {@link #instance}
     */
    public T setInstance(final Instance _instance)
    {
        this.instance = _instance;
        return getThis();
    }

    protected static Map<String, Object> getParameters(final Parameter _parameter)
        throws EFapsException
    {
        return getParameters(_parameter, null);
    }

    protected static Map<String, Object> getParameters(final Parameter _parameter,
                                                       final Instance _docInst)
        throws EFapsException
    {
        Instance docInst = _docInst;
        Instance employeeInst = null;
        if (docInst == null) {
            docInst = _parameter.getInstance();
            if (docInst == null && _parameter.getCallInstance() != null) {
                docInst = _parameter.getCallInstance();
            }
        }
        if (docInst != null) {
            if (docInst.getType().isKindOf(CIPayroll.PositionAbstract)) {
                final PrintQuery print = new PrintQuery(docInst);
                final SelectBuilder sel = SelectBuilder.get()
                                .linkto(CIPayroll.PositionAbstract.DocumentAbstractLink)
                                .linkto(CIPayroll.Payslip.EmployeeAbstractLink).instance();
                print.addSelect(sel);
                print.execute();
                employeeInst = print.getSelect(sel);
            } else if (docInst.getType().isKindOf(CIPayroll.Payslip)) {
                final PrintQuery print = new PrintQuery(docInst);
                final SelectBuilder sel = SelectBuilder.get()
                                .linkto(CIPayroll.Payslip.EmployeeAbstractLink).instance();
                print.addSelect(sel);
                print.execute();
                employeeInst = print.getSelect(sel);
            }
        } else {
            employeeInst = Instance.get(_parameter.getParameterValue("employee"));
        }
        return getParameters(_parameter, docInst, employeeInst);
    }

    protected static Map<String, Object> getParameters(final Parameter _parameter,
                                                       final Instance _docInst,
                                                       final Instance _employeeInst)
        throws EFapsException
    {
        final Map<String, Object> ret = new HashMap<>();

        AbstractParameter.add2Parameters(_parameter, ret, _docInst);
        DateTime date;
        if (ret.containsKey(AbstractParameter.PARAKEY4DATE)) {
            date = (DateTime) ret.get(AbstractParameter.PARAKEY4DATE);
        } else {
            date = new DateTime();
        }
        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.ParameterAbstract);
        queryBldr.addWhereAttrGreaterValue(CIPayroll.ParameterAbstract.ValidUntil, date.minusMinutes(1));
        queryBldr.addWhereAttrLessValue(CIPayroll.ParameterAbstract.ValidFrom, date.plusMinutes(1));

        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIPayroll.ParameterAbstract.Key, CIPayroll.ParameterAbstract.Value);
        multi.execute();
        while (multi.next()) {
            if (multi.getCurrentInstance().getType().isCIType(CIPayroll.ParameterFix)) {
                final FixParameter para = new FixParameter()
                                .setInstance(multi.getCurrentInstance())
                                .setKey(multi.<String>getAttribute(CIPayroll.ParameterAbstract.Key))
                                .setValue(multi.<String>getAttribute(CIPayroll.ParameterAbstract.Value));
                ret.put(para.getKey(), para.getValue());
            } else if (multi.getCurrentInstance().getType().isCIType(CIPayroll.ParameterEmployee)
                            && _employeeInst.isValid()) {
                final EmployeeParameter para = new EmployeeParameter()
                                .setInstance(multi.getCurrentInstance())
                                .setKey(multi.<String>getAttribute(CIPayroll.ParameterAbstract.Key))
                                .setSelect(multi.<String>getAttribute(CIPayroll.ParameterAbstract.Value))
                                .setEmployeeInstance(_employeeInst);
                ret.put(para.getKey(), para.getValue());
            }
        }
        if (_employeeInst != null && _employeeInst.isValid()) {
            ret.put(AbstractParameter.PARAKEY4EMPLOYINST, _employeeInst);
        }
        return ret;
    }


    /**
     * @param _parameter parameter as passed by the eFaps API
     * @param _map map to add to
     * @throws EFapsException on error
     */
    protected static void add2Parameters(final Parameter _parameter,
                                         final Map<String, Object> _map)
        throws EFapsException
    {
        AbstractParameter.add2Parameters(_parameter, _map, null);
    }

    /**
     * @param _parameter parameter as passed by the eFaps API
     * @param _map map to add to
     * @param _docInst instance of the document to be evaluated for parameters
     * @throws EFapsException on error
     */
    protected static void add2Parameters(final Parameter _parameter,
                                         final Map<String, Object> _map,
                                         final Instance _docInst)
        throws EFapsException
    {
        if (!_map.containsKey(AbstractParameter.PARAKEY4DATE)) {
            DateTime date = null;
            if (_docInst != null && _docInst.isValid() && _docInst.getType().isKindOf(CIPayroll.DocumentAbstract)) {
                final PrintQuery print = CachedPrintQuery.get4Request(_docInst);
                print.addAttribute(CIPayroll.DocumentAbstract.Date, CIPayroll.DocumentAbstract.LaborTime,
                                CIPayroll.DocumentAbstract.ExtraLaborTime, CIPayroll.DocumentAbstract.HolidayLaborTime,
                                CIPayroll.DocumentAbstract.NightLaborTime);
                print.executeWithoutAccessCheck();
                date = print.getAttribute(CIPayroll.DocumentAbstract.Date);
            } else {
                final String dateStr = _parameter.getParameterValue("date_eFapsDate");
                if (dateStr != null) {
                    date = DateUtil.getDateFromParameter(dateStr);
                }
            }
            if (date != null) {
                _map.put(AbstractParameter.PARAKEY4DATE, date);
            }
        }
        if (!_map.containsKey(AbstractParameter.PARAKEY4LT)) {
            BigDecimal laborTime = null;
            if (_docInst != null && _docInst.isValid() && _docInst.getType().isKindOf(CIPayroll.DocumentAbstract)) {
                final PrintQuery print = CachedPrintQuery.get4Request(_docInst);
                print.addAttribute(CIPayroll.DocumentAbstract.Date, CIPayroll.DocumentAbstract.LaborTime,
                                CIPayroll.DocumentAbstract.ExtraLaborTime, CIPayroll.DocumentAbstract.HolidayLaborTime,
                                CIPayroll.DocumentAbstract.NightLaborTime);
                print.executeWithoutAccessCheck();
                laborTime = (BigDecimal) print.<Object[]>getAttribute(CIPayroll.DocumentAbstract.LaborTime)[0];
            } else {
                try {
                    final String laborTimeStr = _parameter
                                    .getParameterValue(CIFormPayroll.Payroll_PayslipForm.laborTime.name);
                    if (laborTimeStr != null && !laborTimeStr.isEmpty()) {
                        laborTime = (BigDecimal) NumberFormatter.get().getFormatter().parse(laborTimeStr);
                    } else {
                        laborTime = BigDecimal.ZERO;
                    }
                } catch (final ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
            if (laborTime != null) {
                _map.put(AbstractParameter.PARAKEY4LT, Calculator.toJexlBigDecimal(_parameter, laborTime));
            }
        }
        if (!_map.containsKey(AbstractParameter.PARAKEY4ELT)) {
            BigDecimal laborTime = null;
            if (_docInst != null && _docInst.isValid() && _docInst.getType().isKindOf(CIPayroll.DocumentAbstract)) {
                final PrintQuery print = CachedPrintQuery.get4Request(_docInst);
                print.addAttribute(CIPayroll.DocumentAbstract.Date, CIPayroll.DocumentAbstract.LaborTime,
                                CIPayroll.DocumentAbstract.ExtraLaborTime, CIPayroll.DocumentAbstract.HolidayLaborTime,
                                CIPayroll.DocumentAbstract.NightLaborTime);
                print.executeWithoutAccessCheck();
                laborTime = (BigDecimal) print.<Object[]>getAttribute(CIPayroll.DocumentAbstract.ExtraLaborTime)[0];
            } else {
                try {
                    final String laborTimeStr = _parameter
                                    .getParameterValue(CIFormPayroll.Payroll_PayslipForm.extraLaborTime.name);
                    if (laborTimeStr != null && !laborTimeStr.isEmpty()) {
                        laborTime = (BigDecimal) NumberFormatter.get().getFormatter().parse(laborTimeStr);
                    } else {
                        laborTime = BigDecimal.ZERO;
                    }
                } catch (final ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (laborTime != null) {
                _map.put(AbstractParameter.PARAKEY4ELT, Calculator.toJexlBigDecimal(_parameter, laborTime));
            }
        }
        if (!_map.containsKey(AbstractParameter.PARAKEY4HLT)) {
            BigDecimal laborTime = null;
            if (_docInst != null && _docInst.isValid() && _docInst.getType().isKindOf(CIPayroll.DocumentAbstract)) {
                final PrintQuery print = CachedPrintQuery.get4Request(_docInst);
                print.addAttribute(CIPayroll.DocumentAbstract.Date, CIPayroll.DocumentAbstract.LaborTime,
                                CIPayroll.DocumentAbstract.ExtraLaborTime, CIPayroll.DocumentAbstract.HolidayLaborTime,
                                CIPayroll.DocumentAbstract.NightLaborTime);
                print.executeWithoutAccessCheck();
                laborTime = (BigDecimal) print.<Object[]>getAttribute(CIPayroll.DocumentAbstract.HolidayLaborTime)[0];
            } else {
                try {
                    final String laborTimeStr = _parameter
                                    .getParameterValue(CIFormPayroll.Payroll_PayslipForm.holidayLaborTime.name);
                    if (laborTimeStr != null && !laborTimeStr.isEmpty()) {
                        laborTime = (BigDecimal) NumberFormatter.get().getFormatter().parse(laborTimeStr);
                    } else {
                        laborTime = BigDecimal.ZERO;
                    }
                } catch (final ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (laborTime != null) {
                _map.put(AbstractParameter.PARAKEY4HLT, Calculator.toJexlBigDecimal(_parameter, laborTime));
            }
        }
        if (!_map.containsKey(AbstractParameter.PARAKEY4NLT)) {
            BigDecimal laborTime = null;
            if (_docInst != null && _docInst.isValid() && _docInst.getType().isKindOf(CIPayroll.DocumentAbstract)) {
                final PrintQuery print = CachedPrintQuery.get4Request(_docInst);
                print.addAttribute(CIPayroll.DocumentAbstract.Date, CIPayroll.DocumentAbstract.LaborTime,
                                CIPayroll.DocumentAbstract.ExtraLaborTime, CIPayroll.DocumentAbstract.HolidayLaborTime,
                                CIPayroll.DocumentAbstract.NightLaborTime);
                print.executeWithoutAccessCheck();
                laborTime = (BigDecimal) print.<Object[]>getAttribute(CIPayroll.DocumentAbstract.NightLaborTime)[0];
            } else {
                try {
                    final String laborTimeStr = _parameter
                                    .getParameterValue(CIFormPayroll.Payroll_PayslipForm.nightLaborTime.name);
                    if (laborTimeStr != null && !laborTimeStr.isEmpty()) {
                        laborTime = (BigDecimal) NumberFormatter.get().getFormatter().parse(laborTimeStr);
                    } else {
                        laborTime = BigDecimal.ZERO;
                    }
                } catch (final ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (laborTime != null) {
                _map.put(AbstractParameter.PARAKEY4NLT, Calculator.toJexlBigDecimal(_parameter, laborTime));
            }
        }
    }
}
