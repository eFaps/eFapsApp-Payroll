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
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.esjp.payroll;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.payroll.rules.AbstractRule_Base;


/**
 * This class must be replaced for customization, therefore it is left empty.
 * Functional description can be found in the related "<code>_Base</code>"
 * class.
 *
 * @author The eFaps Team
 * @version $Id: Payslip.java 14839 2015-02-11 01:49:12Z jan@moxter.net $
 */
@EFapsUUID("ba2ebd95-2872-4118-a147-a7f938f967fb")
@EFapsApplication("eFapsApp-Payroll")
public abstract class AbstractAlteration
    extends AbstractAlteration_Base
{

    public static AlterationListener getAlterationListener(final AbstractRule_Base<?> _abstractRule_Base)
    {
        return new AlterationListener(_abstractRule_Base);
    }
}
