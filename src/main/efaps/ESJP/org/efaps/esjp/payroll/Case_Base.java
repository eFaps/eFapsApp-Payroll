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

import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.datamodel.ui.UIInterface;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
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
}
