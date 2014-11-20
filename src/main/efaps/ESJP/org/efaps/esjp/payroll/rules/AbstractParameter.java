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

import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.util.EFapsException;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_Base</code>"
 * class.
 *
 * @author The eFaps Team
 * @version $Id$
 * @param <T> type of parameter
 */
@EFapsUUID("f36f2d26-3dc2-44f1-ba12-c7e099762484")
@EFapsRevision("$Rev$")
public abstract class AbstractParameter<T>
    extends AbstractParameter_Base<T>
{

    /**
     * Key for a parameter containing the instance of the employee.
     */
    public static final String PARAKEY4EMPLOYINST = AbstractParameter_Base.PARAKEY4EMPLOYINST;
    /**
     * Key for a parameter containing the date.
     */
    public static final String PARAKEY4DATE = AbstractParameter_Base.PARAKEY4DATE;
    /**
     * Key for a parameter containing the date.
     */
    public static final String PARAKEY4LT = AbstractParameter_Base.PARAKEY4LT;

    /**
     * Key for a parameter containing the Extra Labor Time.
     */
    public static final String PARAKEY4ELT = AbstractParameter_Base.PARAKEY4ELT;

    /**
     * Key for a parameter containing the Night Labor Time.
     */
    public static final String PARAKEY4HLT = AbstractParameter_Base.PARAKEY4HLT;
    /**
     * Key for a parameter containing the Holiday Labor Time.
     */
    public static final String PARAKEY4NLT = AbstractParameter_Base.PARAKEY4NLT;

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @return Map of parameters
     * @throws EFapsException on error
     */
    public static Map<String, Object> getParameters(final Parameter _parameter)
        throws EFapsException
    {
        return AbstractParameter_Base.getParameters(_parameter);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst instance of the document
     * @return Map of parameters
     * @throws EFapsException on error
     */
    public static Map<String, Object> getParameters(final Parameter _parameter,
                                                    final Instance _docInst)
        throws EFapsException
    {
        return AbstractParameter_Base.getParameters(_parameter, _docInst);
    }

    /**
     * @param _parameter Parameter as passed by the eFaps API
     * @param _docInst instance of the document
     * @param _employeeInst instance of the employee
     * @return Map of parameters
     * @throws EFapsException on error
     */
    public static Map<String, Object> getParameters(final Parameter _parameter,
                                                    final Instance _docInst,
                                                    final Instance _employeeInst)
        throws EFapsException
    {
        return AbstractParameter_Base.getParameters(_parameter, _docInst, _employeeInst);
    }

    /**
     * @param _parameter parameter as passed by the eFaps API
     * @param _map map to add to
     * @throws EFapsException on error
     */
    public static void add2Parameters(final Parameter _parameter,
                                      final Map<String, Object> _map)
        throws EFapsException
    {
        AbstractParameter_Base.add2Parameters(_parameter, _map);
    }

    /**
     * @param _parameter parameter as passed by the eFaps API
     * @param _map map to add to
     * @param _docInst instance of the document to be evaluated for parameters
     * @throws EFapsException on error
     */
    public static void add2Parameters(final Parameter _parameter,
                                      final Map<String, Object> _map,
                                      final Instance _docInst)
        throws EFapsException
    {
        AbstractParameter_Base.add2Parameters(_parameter, _map, _docInst);
    }
}
