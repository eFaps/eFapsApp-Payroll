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
 */
package org.efaps.esjp.payroll;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.esjp.ci.CIPayroll;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.erp.CommonDocument;
import org.efaps.util.EFapsException;

/**
 * The Class Process_Base.
 *
 * @author The eFaps Team
 */
@EFapsUUID("29d33306-c2bf-4f58-a369-c888ac5ca715")
@EFapsApplication("eFapsApp-Payroll")
public abstract class Process_Base
    extends CommonDocument
{

    /**
     * Creates the.
     *
     * @param _parameter the _parameter
     * @return the return
     * @throws EFapsException on error
     */
    public Return create(final Parameter _parameter)
       throws EFapsException
    {
        final Create create = new Create() {
            @Override
            protected void add2basicInsert(final Parameter _parameter,
                                           final Insert _insert)
                                               throws EFapsException
            {
                super.add2basicInsert(_parameter, _insert);
                _insert.add(CIPayroll.ProcessAbstract.Name, getDocName4Create(_parameter));
            }
        };
        return create.execute(_parameter);
    }
}
