/*
 * Copyright 2003 - 2010 The eFaps Team
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


package org.efaps.esjp.payroll;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.datamodel.ui.UIInterface;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.util.EFapsException;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("2af9c234-3b80-4af0-9216-b1dd84179810")
@EFapsRevision("$Rev$")
public abstract class Case_Base
{
    /**
     * Key to store the current case in the session.
     */
    public static final String CASE_SESSIONKEY = "eFaps_Payroll_Case_Session_Key";

    /**
     * Called from field for the case.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return getCaseFieldValue(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String type = (String) properties.get("Type");
        if (type != null) {

            final Map<String, String> values = new TreeMap<String, String>();

            final QueryBuilder queryBldr = new QueryBuilder(Type.get(type));
            queryBldr.addWhereAttrEqValue(CIPayroll.CaseAbstract.Active, true);
            final MultiPrintQuery print = queryBldr.getPrint();
            print.addAttribute(CIPayroll.CaseAbstract.Name);
            print.execute();
            while (print.next()) {
                final String name = print.<String>getAttribute(CIPayroll.CaseAbstract.Name);
                values.put(name, print.getCurrentInstance().getOid());
            }
            final FieldValue fieldvalue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);
            final StringBuilder html = new StringBuilder();
            html.append("<select name=\"").append(fieldvalue.getField().getName()).append("\" ")
                            .append(UIInterface.EFAPSTMPTAG).append(" >");
            boolean first = true;
            for (final Entry<String, String> entry : values.entrySet()) {
                if (first) {
                    Context.getThreadContext().setSessionAttribute(Case_Base.CASE_SESSIONKEY, entry.getValue());
                    first = false;
                }
                html.append("<option value=\"").append(entry.getValue());
                html.append("\">").append(entry.getKey()).append("</option>");
            }
            html.append("</select>");
            ret.put(ReturnValues.SNIPLETT, html.toString());
        }
        return ret;
    }

    public Return copy(final Parameter _parameter)
        throws EFapsException
    {
        final String[] oids = (String[]) _parameter.get(ParameterValues.OTHERS);
        final Instance oldCaseInst = Instance.get(oids[0]);
        final PrintQuery print = new PrintQuery(oldCaseInst);
        print.addAttribute(CIPayroll.CasePayslip.Name,
                        CIPayroll.CasePayslip.Description,
                        CIPayroll.CasePayslip.Active);
        print.execute();
        final String caseName = print.<String>getAttribute(CIPayroll.CasePayslip.Name);
        final String caseDesc = print.<String>getAttribute(CIPayroll.CasePayslip.Description);
        final Boolean caseActive = print.<Boolean>getAttribute(CIPayroll.CasePayslip.Active);

        final Insert insert = new Insert(CIPayroll.CasePayslip);
        insert.add(CIPayroll.CasePayslip.Name, caseName);
        insert.add(CIPayroll.CasePayslip.Description, caseDesc);
        insert.add(CIPayroll.CasePayslip.Active, caseActive);
        insert.execute();

        final Instance newCaseInst = insert.getInstance();

        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.CasePositionRootSum);
        queryBldr.addWhereAttrEqValue(CIPayroll.CasePositionRootSum.CaseLink, oldCaseInst.getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIPayroll.CasePositionRootSum.Name,
                        CIPayroll.CasePositionRootSum.Description,
                        CIPayroll.CasePositionRootSum.Mode,
                        CIPayroll.CasePositionRootSum.Sorted);
        multi.execute();
        while (multi.next()) {
            final Instance oldParentInst = multi.getCurrentInstance();

            final Insert insertRoot = new Insert(CIPayroll.CasePositionRootSum);
            insertRoot.add(CIPayroll.CasePositionRootSum.CaseLink, newCaseInst.getId());
            insertRoot.add(CIPayroll.CasePositionRootSum.Name,
                            multi.<String>getAttribute(CIPayroll.CasePositionRootSum.Name) + " _copy");
            insertRoot.add(CIPayroll.CasePositionRootSum.Description,
                            multi.getAttribute(CIPayroll.CasePositionRootSum.Description));
            insertRoot.add(CIPayroll.CasePositionRootSum.Mode, multi.getAttribute(CIPayroll.CasePositionRootSum.Mode));
            insertRoot.add(CIPayroll.CasePositionRootSum.Sorted,
                            multi.getAttribute(CIPayroll.CasePositionRootSum.Sorted));
            insertRoot.execute();

            final Instance newParentInst = insertRoot.getInstance();
            final Map<Instance, Instance> instanceMap = new HashMap<Instance, Instance>();
            insertCasePosition(instanceMap, oldCaseInst, newCaseInst, oldParentInst, newParentInst);
            insertCasePosition2Position(instanceMap);
        }

        return new Return();
    }

    private void insertCasePosition(final Map<Instance, Instance> _instanceMap,
                                    final Instance _oldCaseInst,
                                    final Instance _newCaseInst,
                                    final Instance _oldParentInst,
                                    final Instance _newParentInst)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.CasePositionAbstract);
        queryBldr.addWhereAttrEqValue(CIPayroll.CasePositionAbstract.CaseAbstractLink, _oldCaseInst.getId());
        queryBldr.addWhereAttrEqValue(CIPayroll.CasePositionAbstract.ParentAbstractLink, _oldParentInst.getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIPayroll.CasePositionAbstract.Name,
                        CIPayroll.CasePositionAbstract.Description,
                        CIPayroll.CasePositionAbstract.Mode,
                        CIPayroll.CasePositionAbstract.ParentAbstractLink,
                        CIPayroll.CasePositionAbstract.Sorted,
                        CIPayroll.CasePositionCalc.CalculatorESJP,
                        CIPayroll.CasePositionCalc.CalculatorMethod,
                        CIPayroll.CasePositionCalc.DefaultValue,
                        CIPayroll.CasePositionCalc.ExportAFP,
                        CIPayroll.CasePositionCalc.ExportReport,
                        CIPayroll.CasePositionCalc.ActionDefinitionLink);
        multi.execute();
        while (multi.next()) {
            final Insert insert = new Insert(multi.getCurrentInstance().getType());
            insert.add(CIPayroll.CasePositionAbstract.CaseAbstractLink, _newCaseInst.getId());
            insert.add(CIPayroll.CasePositionAbstract.ParentAbstractLink, _newParentInst.getId());
            insert.add(CIPayroll.CasePositionAbstract.Name, multi.getAttribute(CIPayroll.CasePositionAbstract.Name));
            insert.add(CIPayroll.CasePositionAbstract.Description,
                            multi.getAttribute(CIPayroll.CasePositionAbstract.Description));
            insert.add(CIPayroll.CasePositionAbstract.Mode, multi.getAttribute(CIPayroll.CasePositionAbstract.Mode));
            insert.add(CIPayroll.CasePositionAbstract.Sorted, multi.getAttribute(CIPayroll.CasePositionAbstract.Sorted));
            if (multi.getCurrentInstance().getType().isKindOf(CIPayroll.CasePositionCalc.getType())) {
                insert.add(CIPayroll.CasePositionCalc.CalculatorESJP,
                                multi.getAttribute(CIPayroll.CasePositionCalc.CalculatorESJP));
                insert.add(CIPayroll.CasePositionCalc.CalculatorMethod,
                                multi.getAttribute(CIPayroll.CasePositionCalc.CalculatorMethod));
                insert.add(CIPayroll.CasePositionCalc.DefaultValue,
                                multi.getAttribute(CIPayroll.CasePositionCalc.DefaultValue));
                insert.add(CIPayroll.CasePositionCalc.ExportAFP,
                                multi.getAttribute(CIPayroll.CasePositionCalc.ExportAFP));
                insert.add(CIPayroll.CasePositionCalc.ExportReport,
                                multi.getAttribute(CIPayroll.CasePositionCalc.ExportReport));
                insert.add(CIPayroll.CasePositionCalc.ActionDefinitionLink,
                                multi.getAttribute(CIPayroll.CasePositionCalc.ActionDefinitionLink));
            }
            insert.execute();
            _instanceMap.put(multi.getCurrentInstance(), insert.getInstance());
            insertCasePosition(_instanceMap, _oldCaseInst, _newCaseInst, multi.getCurrentInstance(),
                            insert.getInstance());
        }
    }

    private void insertCasePosition2Position(final Map<Instance, Instance> instanceMap)
        throws EFapsException
    {
        for (final Entry<Instance, Instance> entry : instanceMap.entrySet()) {
            if (entry.getKey().getType().isKindOf(CIPayroll.CasePositionCalc.getType())) {
                final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.CasePosition2PositionAbstract);
                queryBldr.addWhereAttrEqValue(CIPayroll.CasePosition2PositionAbstract.FromAbstractLink, entry.getKey()
                                .getId());
                final MultiPrintQuery multi = queryBldr.getPrint();
                multi.addAttribute(CIPayroll.CasePosition2PositionAbstract.Numerator,
                                CIPayroll.CasePosition2PositionAbstract.Denominator,
                                CIPayroll.CasePositionCalc2Position4Parameter.Parameter);
                final SelectBuilder selToLinkInst = new SelectBuilder()
                                .linkto(CIPayroll.CasePosition2PositionAbstract.ToAbstractLink).instance();
                multi.addSelect(selToLinkInst);
                multi.execute();
                while (multi.next()) {
                    final Instance toLinkInst = multi.<Instance>getSelect(selToLinkInst);
                    final Insert insert = new Insert(multi.getCurrentInstance().getType());
                    insert.add(CIPayroll.CasePosition2PositionAbstract.FromAbstractLink, entry.getValue().getId());
                    insert.add(CIPayroll.CasePosition2PositionAbstract.ToAbstractLink, instanceMap.get(toLinkInst)
                                    .getId());
                    insert.add(CIPayroll.CasePosition2PositionAbstract.Numerator,
                                    multi.getAttribute(CIPayroll.CasePosition2PositionAbstract.Numerator));
                    insert.add(CIPayroll.CasePosition2PositionAbstract.Denominator,
                                    multi.getAttribute(CIPayroll.CasePosition2PositionAbstract.Denominator));
                    if (multi.getCurrentInstance().getType()
                                    .isKindOf(CIPayroll.CasePositionCalc2Position4Parameter.getType())) {
                        insert.add(CIPayroll.CasePositionCalc2Position4Parameter.Parameter,
                                        multi.getAttribute(CIPayroll.CasePositionCalc2Position4Parameter.Parameter));
                    }
                    insert.execute();
                }
            }
        }
    }
}
