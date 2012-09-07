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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractCommand;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.util.EFapsException;



/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("55f00ed3-4110-4e5d-b57c-9f1ee9f983ba")
@EFapsRevision("$Rev$")
public abstract class CasePosition_Base
{
    public enum MODE {
        DEAVTIVATED,
        OPTIONAL,
        OPTIONAL_DEFAULT,
        REQUIRED_EDITABLE,
        REQUIRED_NOEDITABLE;

        public String getLabel() {
            return DBProperties.getProperty("CasePosition.MODE." + ordinal());
        }
    }

    /**
     * Create a position.
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
        throws EFapsException
    {
        new BitSet();

        final AbstractCommand command = (AbstractCommand) _parameter.get(ParameterValues.UIOBJECT);
        final Instance parent = _parameter.getInstance();
        final long caseID;
        if (parent.getType().isKindOf(CIPayroll.CaseAbstract.getType())) {
            caseID =  parent.getId();
        } else {
            final PrintQuery print = new PrintQuery(parent);
            print.addAttribute(CIPayroll.CasePositionAbstract.CaseAbstractLink);
            print.execute();
            caseID = print.<Long>getAttribute(CIPayroll.CasePositionAbstract.CaseAbstractLink);
        }
        int pos = -1;
        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.CasePositionAbstract);
        queryBldr.addWhereAttrEqValue(CIPayroll.CasePositionAbstract.CaseAbstractLink, caseID);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIPayroll.CasePositionAbstract.Sorted);
        multi.execute();
        while (multi.next()) {
            final Integer tmp = multi.<Integer>getAttribute(CIPayroll.CasePositionAbstract.Sorted);
            if (tmp > pos) {
                pos = tmp;
            }
        }
        pos++;
        final Insert insert = new Insert(command.getTargetCreateType());
        insert.add(CIPayroll.CasePositionAbstract.Name, _parameter.getParameterValue("name"));
        insert.add(CIPayroll.CasePositionAbstract.Description, _parameter.getParameterValue("description"));
        insert.add(CIPayroll.CasePositionAbstract.Mode, _parameter.getParameterValue("mode"));
        insert.add(CIPayroll.CasePositionAbstract.CaseAbstractLink, caseID);
        insert.add(CIPayroll.CasePositionAbstract.Sorted, pos);
        if (!parent.getType().isKindOf(CIPayroll.CaseAbstract.getType())) {
            insert.add(CIPayroll.CasePositionAbstract.ParentAbstractLink, parent.getId());
        }
        if ( _parameter.getParameterValue("actionDefinitionLink") != null
                        && !_parameter.getParameterValue("actionDefinitionLink").isEmpty()
                        && !"null".equals(_parameter.getParameterValue("actionDefinitionLink"))) {
            insert.add(CIPayroll.CasePositionCalc.ActionDefinitionLink,
                            _parameter.getParameterValue("actionDefinitionLink"));
        }
        insert.execute();

        return new Return();
    }

    /**
     * Get the html snipplet for the field continaing the mode.
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return modeFieldValueUI(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final FieldValue fieldValue = (FieldValue) _parameter.get(ParameterValues.UIOBJECT);

        final StringBuilder html = new StringBuilder();
        final Integer ordinal = (Integer) fieldValue.getValue();
        if (TargetMode.CREATE.equals(_parameter.get(ParameterValues.ACCESSMODE))
                        || TargetMode.EDIT.equals(_parameter.get(ParameterValues.ACCESSMODE))) {
            for (final MODE mode : CasePosition.MODE.values()) {
                html.append("<input type=\"radio\" name=\"").append(fieldValue.getField().getName())
                    .append("\" value=\"").append(mode.ordinal()).append("\" ");
                if (new Integer(mode.ordinal()).equals(ordinal)) {
                    html.append("checked=\"checked\"");
                }
                html.append(">").append(mode.getLabel()).append("<br/>");
            }
        } else {
            html.append(CasePosition.MODE.values()[ordinal].getLabel());
        }
        ret.put(ReturnValues.SNIPLETT, html);
        return ret;
    }

    /**
     * Move a caseposition one position up or down.
     * @param _parameter Parameter as passed from the eFaps API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return move(final Parameter _parameter)
        throws EFapsException
    {
        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        boolean up = false;
        if ("true".equalsIgnoreCase((String) properties.get("up"))) {
            up = true;
        }
        int max = 0;
        final Map<Integer, String> sorted2oid = new HashMap<Integer, String>();
        final Map<String, Integer> oid2sorted = new HashMap<String, Integer>();
        final QueryBuilder queryBldr = new QueryBuilder(CIPayroll.CasePositionAbstract);
        queryBldr.addWhereAttrEqValue(CIPayroll.CasePositionAbstract.CaseAbstractLink,
                        _parameter.getInstance().getId());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIPayroll.CasePositionAbstract.Sorted);
        multi.execute();
        while (multi.next()) {
            final Integer tmp = multi.<Integer>getAttribute(CIPayroll.CasePositionAbstract.Sorted);
            sorted2oid.put(tmp, multi.getCurrentInstance().getOid());
            oid2sorted.put(multi.getCurrentInstance().getOid(), tmp);
            if (tmp > max) {
                max = tmp;
            }
        }
        final String[] selected = (String[]) _parameter.get(ParameterValues.OTHERS);
        final String selOID = selected[0];
        String targetOID = null;
        Integer selSort = oid2sorted.get(selOID);

        while (selSort > -1 && selSort < max + 1) {
            if (up) {
                selSort--;
            } else {
                selSort++;
            }
            if (sorted2oid.containsKey(selSort)) {
                targetOID = sorted2oid.get(selSort);
                break;
            }
        }

        if (targetOID != null) {
            final Update updateSel = new Update(selOID);
            updateSel.add(CIPayroll.CasePositionAbstract.Sorted, oid2sorted.get(targetOID));
            updateSel.execute();

            final Update updateTarget = new Update(targetOID);
            updateTarget.add(CIPayroll.CasePositionAbstract.Sorted, oid2sorted.get(selOID));
            updateTarget.execute();
        }
        return new Return();
    }

    /**
     * Executed on autocomplete event for the caseposition field.
     * @param _parameter Parameter as passed from the eFaps API
     * @return values for autocomplete
     * @throws EFapsException on error
     */
    public Return autoComplete4CasePosition(final Parameter _parameter)
        throws EFapsException
    {
        final String caseOid = (String) Context.getThreadContext().getSessionAttribute(Case_Base.CASE_SESSIONKEY);
        final Instance caseInst = Instance.get(caseOid);

        final Map<?, ?> properties = (Map<?, ?>) _parameter.get(ParameterValues.PROPERTIES);
        final String postfix = (String) properties.get("FieldPostfix");
        final String typeStr = (String) properties.get("Type");
        final String modeStr = (String) properties.get("Mode");

        final Map<String, Map<String, String>> sortMap = new TreeMap<String, Map<String, String>>();
        final QueryBuilder queryBldr = new QueryBuilder(Type.get(typeStr));
        queryBldr.addWhereAttrEqValue(CIPayroll.CasePositionCalc.CaseAbstractLink, caseInst.getId());
        if (modeStr != null) {
            queryBldr.addWhereAttrEqValue(CIPayroll.CasePositionCalc.Mode, (Object[]) modeStr.split(";"));
        }
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIPayroll.CasePositionAbstract.Name,
                           CIPayroll.CasePositionAbstract.Description);
        multi.execute();

        while (multi.next()) {
            final String name = multi.getAttribute(CIPayroll.CasePositionAbstract.Name);
            final String descr = multi.getAttribute(CIPayroll.CasePositionAbstract.Description);
            final String oid = multi.getCurrentInstance().getOid();
            final Map<String, String> map = new HashMap<String, String>();
            final String choice = name + " - " + descr;
            map.put("eFapsAutoCompleteKEY", oid);
            map.put("eFapsAutoCompleteVALUE", name);
            map.put("eFapsAutoCompleteCHOICE", choice);
            map.put("description_" + postfix, descr);
            sortMap.put(choice, map);
        }

        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        list.addAll(sortMap.values());
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    /**
     * Executed on update event for the caseposition field.
     * @param _parameter Parameter as passed from the eFaps API
     * @return values to be updated
     * @throws EFapsException on error
     */
    public Return update4CasePosition(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        return ret;
    }

}
