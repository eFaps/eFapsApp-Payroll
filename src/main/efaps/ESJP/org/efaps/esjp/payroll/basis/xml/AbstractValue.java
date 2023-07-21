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
package org.efaps.esjp.payroll.basis.xml;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIAttribute;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

/**
 * The Class AbstractValue.
 *
 * @author The eFaps Team
 * @param <T> the generic type
 */
@EFapsUUID("28c5d620-f874-43ef-9ce6-e1daa64db6aa")
@EFapsApplication("eFapsApp-Payroll")
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractValue<T>
{

    /** The label key. */
    @XmlAttribute(name = "attribute")
    private String attribute;

    /**
     * Gets the label key.
     *
     * @return the label key
     */
    public String getAttribute()
    {
        return this.attribute;
    }

    /**
     * Sets the label key.
     *
     * @param _labelKey the new label key
     */
    public AbstractValue<T> setAttribute(final String _attribute)
    {
        this.attribute = _attribute;
        return this;
    }

    /**
     * Sets the attribute.
     *
     * @param _attribute the _attribute
     * @return the abstract value
     */
    public AbstractValue<?> setAttribute(final CIAttribute _attribute)
    {
        setAttribute(_attribute.ciType.getType().getAttribute(_attribute.name).getKey());
        return this;
    }

    /**
     * Gets the object.
     *
     * @return the value
     */
    public abstract T getObject();

    /**
     * Gets the html.
     *
     * @return the html
     */
    public abstract String getHtml();
}
