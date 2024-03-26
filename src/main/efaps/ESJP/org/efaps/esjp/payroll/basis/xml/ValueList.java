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
package org.efaps.esjp.payroll.basis.xml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * The Class Values.
 *
 * @author The eFaps Team
 */
@EFapsUUID("80561bec-8087-4ad5-978d-4181d9bcc291")
@EFapsApplication("eFapsApp-Payroll")
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
public class ValueList
    implements Serializable
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** The values. */
    @XmlElementWrapper(name = "values")
    @XmlElement(name = "value")
    private List<AbstractValue<?>> values = new ArrayList<>();

    /**
     * Gets the values.
     *
     * @return the values
     */
    public List<AbstractValue<?>> getValues()
    {
        return this.values;
    }

    /**
     * Sets the values.
     *
     * @param _values the new values
     */
    public void setValues(final List<AbstractValue<?>> _values)
    {
        this.values = _values;
    }
}
