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
import java.math.BigDecimal;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * The Class StringValue.
 *
 * @author The eFaps Team
 */
@EFapsUUID("03d5bb55-bb15-4248-b623-759f5e10b43d")
@EFapsApplication("eFapsApp-Payroll")
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
public class DecimalValue
    extends AbstractValue<BigDecimal>
    implements Serializable
{

   /** The Constant serialVersionUID. */
   private static final long serialVersionUID = 1L;

    /** The object. */
    @XmlAttribute(name = "object")
    private BigDecimal object;

    /**
     * Sets the object.
     *
     * @param _object the new object
     */
    public DecimalValue setObject(final BigDecimal _object)
    {
        this.object = _object;
        return this;
    }

    @Override
    public BigDecimal getObject()
    {
        return this.object;
    }

    @Override
    public String getHtml()
    {
        return this.object.toString();
    }
}
