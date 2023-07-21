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

import java.io.Serializable;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * The Class DateValue.
 *
 * @author The eFaps Team
 */
@EFapsUUID("d0aae108-fafe-4b04-8745-c42974c30a54")
@EFapsApplication("eFapsApp-Payroll")
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
public class DateValue
    extends AbstractValue<DateTime>
    implements Serializable
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /** The object. */
    @XmlAttribute(name = "object")
    private String object;

    /**
     * Sets the object.
     *
     * @param _object the new object
     */
    public DateValue setObject(final DateTime _object)
    {
        this.object = _object.toString();
        return this;
    }

    @Override
    public DateTime getObject()
    {
        return new DateTime(this.object);
    }

    @Override
    public String getHtml()
    {
        String ret = "";
        try {
            ret = new DateTime(this.object).toString(DateTimeFormat.forStyle("S-").withLocale(Context.getThreadContext()
                            .getLocale()));
        } catch (final EFapsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }
}
