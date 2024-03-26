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
//CHECKSTYLE:OFF
package org.efaps.esjp.ci;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsNoUpdate;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIAttribute;
import org.efaps.ci.CIStatus;
import org.efaps.ci.CIType;

/**
 * This class is only used in case that the Projects App is not installed to be
 * able to compile the classes.
 *
 * @author The eFaps Team
 */
@EFapsUUID("99663e97-4235-4a0f-9272-64596b8492b0")
@EFapsApplication("eFapsApp-Payroll")
@EFapsNoUpdate
public final class CIProjects
{

    public static final _Project2DocumentAbstract Project2DocumentAbstract = new _Project2DocumentAbstract(
                    "a6accf51-06d0-4882-a4c7-617cd5bf789b");

    public static class _Project2DocumentAbstract
        extends CIType
    {

        protected _Project2DocumentAbstract(final String _uuid)
        {
            super(_uuid);
        }

        public final CIAttribute Created = new CIAttribute(this, "Created");
        public final CIAttribute Creator = new CIAttribute(this, "Creator");
        public final CIAttribute FromAbstract = new CIAttribute(this, "FromAbstract");
        public final CIAttribute Modified = new CIAttribute(this, "Modified");
        public final CIAttribute Modifier = new CIAttribute(this, "Modifier");
        public final CIAttribute ToAbstract = new CIAttribute(this, "ToAbstract");
    }

    public static final _ProjectService2DocumentAbstract ProjectService2DocumentAbstract = new _ProjectService2DocumentAbstract(
                    "bcb41bad-a349-4012-9cb8-2afd16830aa3");

    public static class _ProjectService2DocumentAbstract
        extends _Project2DocumentAbstract
    {

        protected _ProjectService2DocumentAbstract(final String _uuid)
        {
            super(_uuid);
        }

        public final CIAttribute FromService = new CIAttribute(this, "FromService");
        public final CIAttribute ToDocument = new CIAttribute(this, "ToDocument");
    }

    public static final _ProjectService2Employee ProjectService2Employee = new _ProjectService2Employee(
                    "967e7a7a-2f7f-4462-aead-e8633e44b871");

    public static class _ProjectService2Employee
        extends org.efaps.esjp.ci.CIERP._Object2ObjectAbstract
    {

        protected _ProjectService2Employee(final String _uuid)
        {
            super(_uuid);
        }

        public final CIAttribute FromLink = new CIAttribute(this, "FromLink");
        public final CIAttribute Status = new CIAttribute(this, "Status");
        public final CIAttribute ToLink = new CIAttribute(this, "ToLink");
    }

    public static final _ProjectService2EmployeeStatus ProjectService2EmployeeStatus = new _ProjectService2EmployeeStatus(
                    "3b4f6743-1b59-4d1f-8c98-4a0f1e44c8c1");

    public static class _ProjectService2EmployeeStatus
        extends org.efaps.esjp.ci.CIAdmin._DataModel_StatusAbstract
    {

        protected _ProjectService2EmployeeStatus(final String _uuid)
        {
            super(_uuid);
        }

        public final CIStatus Active = new CIStatus(this, "Active");
        public final CIStatus Inactive = new CIStatus(this, "Inactive");
    }
}
