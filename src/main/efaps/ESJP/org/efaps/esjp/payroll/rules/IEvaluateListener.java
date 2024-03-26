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
package org.efaps.esjp.payroll.rules;

import org.apache.commons.jexl2.JexlContext;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: $
 */
@EFapsUUID("e0c7696e-0cec-45b0-a43e-c578b9d2fea7")
@EFapsApplication("eFapsApp-Payroll")
public interface IEvaluateListener
    extends IRuleListener
{

    /**
     * @param _jexlContext context of jexl executed
     * @param _val value object
     * @return the value
     * @throws EFapsException on error
     */
    Object onEvaluate(final JexlContext _jexlContext,
                      final Object _val)
        throws EFapsException;
}
