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

import java.io.Serializable;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("b135785e-ee97-4756-8c95-fbcc22230a71")
@EFapsRevision("$Rev$")
public abstract class MessageLog_Base
    implements Log, Serializable
{
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Logger for this classes.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MessageLog.class);

    /**
     * Stack of warnings.
     */
    private final Stack<String> warnstack = new Stack<>();

    @Override
    public boolean isDebugEnabled()
    {
        return LOG.isDebugEnabled();
    }

    @Override
    public boolean isErrorEnabled()
    {
        return LOG.isErrorEnabled();
    }

    @Override
    public boolean isFatalEnabled()
    {

        return isErrorEnabled();
    }

    @Override
    public boolean isInfoEnabled()
    {

        return LOG.isInfoEnabled();
    }

    @Override
    public boolean isTraceEnabled()
    {
        return LOG.isTraceEnabled();
    }

    @Override
    public boolean isWarnEnabled()
    {
        return LOG.isWarnEnabled();
    }

    @Override
    public void trace(final Object _message)
    {
        LOG.trace(_message.toString());
    }

    @Override
    public void trace(final Object _message,
                      final Throwable _t)
    {
        LOG.trace(_message.toString(), _t);
    }

    @Override
    public void debug(final Object _message)
    {
        LOG.debug(_message.toString());
    }

    @Override
    public void debug(final Object _message,
                      final Throwable _t)
    {
        LOG.debug(_message.toString(), _t);
    }

    @Override
    public void info(final Object _message)
    {
        LOG.info(_message.toString());
    }

    @Override
    public void info(final Object _message,
                     final Throwable _t)
    {
        LOG.info(_message.toString(), _t);
    }

    @Override
    public void warn(final Object _message)
    {
        String msgTmp = _message.toString();
        msgTmp = msgTmp.replaceFirst("^.*: ", "");
        this.warnstack.push(msgTmp);
        LOG.warn(_message.toString());
    }

    @Override
    public void warn(final Object _message,
                     final Throwable _t)
    {
        LOG.warn(_message.toString(), _t);
    }

    @Override
    public void error(final Object _message)
    {
        LOG.error(_message.toString());
    }

    @Override
    public void error(final Object _message,
                      final Throwable _t)
    {
        LOG.error(_message.toString(), _t);
    }

    @Override
    public void fatal(final Object _message)
    {
        this.error(_message);
    }

    @Override
    public void fatal(final Object _message,
                      final Throwable _t)
    {
        this.error(_message, _t);
    }

    /**
     * @return message to be shown
     */
    public String getMessage()
    {
        String ret;
        if (!this.warnstack.isEmpty()) {
            ret = this.warnstack.pop();
        } else {
            ret = "";
        }
        return ret;
    }

    /**
     *
     */
    public void clean()
    {
        this.warnstack.clear();
    }
}
