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

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIAttribute;
import org.efaps.db.Instance;
import org.efaps.esjp.payroll.basis.xml.ValueList;
import org.efaps.util.EFapsException;

/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_Base</code>"
 * class.
 *
 * @author The eFaps Team
 */
@EFapsUUID("6c72c312-02a6-4ac7-8354-3827db512e51")
@EFapsApplication("eFapsApp-Payroll")
public class BasisAttribute
    extends BasisAttribute_Base
{

    /**
     * Gets the value list for doc.
     *
     * @param _parameter the _parameter
     * @param _inst the _inst
     * @return the value list4 doc
     * @throws EFapsException on error
     */
    public static ValueList getValueList4Inst(final Parameter _parameter,
                                              final Instance _inst)
        throws EFapsException
    {
        return BasisAttribute_Base.getValueList4Inst(_parameter, _inst);
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
    public static Object getObjectValue(final Parameter _parameter,
                                        final ValueList _valueList,
                                        final CIAttribute _attribute,
                                        final Object _aternativeValue)
    {
        return BasisAttribute_Base.getObjectValue(_parameter, _valueList, _attribute, _aternativeValue);
    }
}
