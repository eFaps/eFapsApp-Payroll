/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.payroll.basis;

import java.math.BigDecimal;
import java.util.List;

import org.efaps.admin.datamodel.Attribute;
import org.efaps.admin.datamodel.IEnum;
import org.efaps.admin.datamodel.IJaxb;
import org.efaps.admin.datamodel.ui.UIValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.ci.CIAttribute;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.payroll.basis.xml.AbstractValue;
import org.efaps.esjp.payroll.basis.xml.DateValue;
import org.efaps.esjp.payroll.basis.xml.DecimalValue;
import org.efaps.esjp.payroll.basis.xml.StringValue;
import org.efaps.esjp.payroll.basis.xml.ValueList;
import org.efaps.esjp.ui.html.Table;
import org.efaps.ui.wicket.util.EnumUtil;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("396cf26e-df12-4429-95e6-6d0b25bb81e3")
@EFapsApplication("eFapsApp-Payroll")
public abstract class BasisAttribute_Base
    implements IJaxb
{

    @Override
    public Class<?>[] getClasses()
    {
        return new Class<?>[] { ValueList.class, StringValue.class, DateValue.class, DecimalValue.class };
    }

    @Override
    public String getUISnipplet(final TargetMode _mode,
                                final UIValue _value)
    {
        final ValueList val = (ValueList) _value.getDbValue();
        final StringBuilder ret = new StringBuilder();
        if (val != null) {
            final Table table = new Table();
            for (final AbstractValue<?> value : val.getValues()) {
                Attribute attr;
                try {
                    attr = Attribute.get(value.getAttribute());
                    table.addRow().addColumn(attr == null ? "" : DBProperties.getProperty(attr.getLabelKey()))
                                    .addColumn(value.getHtml());
                } catch (final CacheReloadException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            ret.append(table.toHtml());
        }
        return ret.toString();
    }

    /**
     * Gets the value list4 inst.
     *
     * @param _parameter the _parameter
     * @param _inst the _inst
     * @return the value list4 inst
     * @throws EFapsException on error
     */
    protected static ValueList getValueList4Inst(final Parameter _parameter,
                                                 final Instance _inst)
        throws EFapsException
    {
        final ValueList ret = new ValueList();
        Instance emplInst = null;
        if (_inst.getType().isKindOf(CIHumanResource.EmployeeAbstract)) {
            emplInst = _inst;
        } else if (_inst.getType().isKindOf(CIPayroll.DocumentAbstract)) {
            final PrintQuery print = new PrintQuery(_inst);
            final SelectBuilder selEmplInst = SelectBuilder.get().linkto(
                            CIPayroll.DocumentAbstract.EmployeeAbstractLink).instance();
            print.addSelect(selEmplInst);
            print.execute();
            emplInst = print.getSelect(selEmplInst);
        }
        if (emplInst != null && emplInst.isValid()) {
            final PrintQuery print = new PrintQuery(emplInst);
            final SelectBuilder selEmplRem = SelectBuilder.get().clazz(CIHumanResource.ClassTR_Labor).attribute(
                            CIHumanResource.ClassTR_Labor.Remuneration);
            final SelectBuilder selEmplPR = SelectBuilder.get().clazz(CIHumanResource.ClassTR_Health).linkto(
                            CIHumanResource.ClassTR_Health.PensionRegimeLink).attribute(
                                            CIHumanResource.AttributeDefinitionPensionRegime.Value);
            final SelectBuilder selEmplPRT = SelectBuilder.get().clazz(CIHumanResource.ClassTR_Health).linkto(
                            CIHumanResource.ClassTR_Health.PensionRegimeTypeLink).attribute(
                                            CIHumanResource.AttributeDefinitionPensionRegimeType.Value);
            final SelectBuilder selEmplST = SelectBuilder.get().clazz(CIHumanResource.ClassTR).attribute(
                            CIHumanResource.ClassTR.StartDate);
            final SelectBuilder selPeri = SelectBuilder.get().clazz(CIHumanResource.ClassTR_Labor).linkto(
                            CIHumanResource.ClassTR_Labor.PeriodicityLink).attribute(
                                            CIHumanResource.AttributeDefinitionPeriodicity.Value);
            final SelectBuilder selEmplEmpl = SelectBuilder.get().linkto(CIHumanResource.Employee.EmployLink).attribute(
                            CIHumanResource.AttributeDefinitionEmploy.Value);
            final SelectBuilder selEmplAct = SelectBuilder.get().attribute(CIHumanResource.Employee.Activation);
            print.addSelect(selEmplRem, selEmplPR, selEmplPRT, selEmplST, selPeri, selEmplEmpl, selEmplAct);
            print.executeWithoutAccessCheck();

            final BigDecimal emplRem = print.getSelect(selEmplRem);
            if (emplRem != null) {
                ret.getValues().add(new DecimalValue().setObject(emplRem).setAttribute(
                                CIHumanResource.ClassTR_Labor.Remuneration));
            }

            final String emplPR = print.getSelect(selEmplPR);
            if (emplPR != null && !emplPR.isEmpty()) {
                ret.getValues().add(new StringValue().setObject(emplPR).setAttribute(
                                CIHumanResource.ClassTR_Health.PensionRegimeLink));
            }

            final String emplPRT = print.getSelect(selEmplPRT);
            if (emplPRT != null && !emplPRT.isEmpty()) {
                ret.getValues().add(new StringValue().setObject(emplPRT).setAttribute(
                                CIHumanResource.ClassTR_Health.PensionRegimeTypeLink));
            }

            final DateTime emplST = print.getSelect(selEmplST);
            if (emplST != null) {
                ret.getValues().add(new DateValue().setObject(emplST).setAttribute(CIHumanResource.ClassTR.StartDate));
            }

            final String peri = print.getSelect(selPeri);
            if (peri != null && !peri.isEmpty()) {
                ret.getValues().add(new StringValue().setObject(peri).setAttribute(
                                CIHumanResource.ClassTR_Labor.PeriodicityLink));
            }

            final String emplEmpl = print.getSelect(selEmplEmpl);
            if (emplEmpl != null && !emplEmpl.isEmpty()) {
                ret.getValues().add(new StringValue().setObject(emplEmpl).setAttribute(
                                CIHumanResource.EmployeeAbstract.EmployLink));
            }

            final Object emplAct = print.getSelect(selEmplAct);
            if (emplAct != null && emplAct instanceof List) {
                final StringBuilder bldr = new StringBuilder();
                boolean first = true;
                for (final Object obj : (List<?>) emplAct) {
                    if (first) {
                        first = false;
                    } else {
                        bldr.append(", ");
                    }
                    bldr.append(EnumUtil.getUILabel((IEnum) obj));
                }
                ret.getValues().add(new StringValue().setObject(bldr.toString()).setAttribute(
                                CIHumanResource.Employee.Activation));
            }
        }
        return ret;
    }


    /**
     * Gets the object value.
     *
     * @param _parameter the _parameter
     * @param _valueList the _value list
     * @param _attribute the _attribute
     * @param _aternativeValue the _aternative value
     * @return the object value
     */
    protected static Object getObjectValue(final Parameter _parameter,
                                           final ValueList _valueList,
                                           final CIAttribute _attribute,
                                           final Object _aternativeValue)
    {
        Object ret = _aternativeValue;
        if ((_valueList != null && !_valueList.getValues().isEmpty())) {
            final String attr = _attribute.ciType.getType().getAttribute(_attribute.name).getKey();
            for (final AbstractValue<?> value : _valueList.getValues()) {
                if (value.getAttribute().equals(attr)) {
                    ret = value.getObject();
                    break;
                }
            }
        }
        return ret;
    }
}
