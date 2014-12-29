//CHECKSTYLE:OFF
package org.efaps.esjp.ci;
import org.efaps.admin.program.esjp.EFapsNoUpdate;
import org.efaps.ci.CIAttribute;
import org.efaps.ci.CIType;


/**
 * This class is only used in case that the Projects App is not installed
 * to be able to compile the classes.
 * @author The eFaps Team
 */
@EFapsNoUpdate
public final class CIProjects
{

    public static final _Project2DocumentAbstract Project2DocumentAbstract = new _Project2DocumentAbstract("a6accf51-06d0-4882-a4c7-617cd5bf789b");
    public static class _Project2DocumentAbstract extends CIType
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

    public static final _ProjectService2DocumentAbstract ProjectService2DocumentAbstract = new _ProjectService2DocumentAbstract("bcb41bad-a349-4012-9cb8-2afd16830aa3");
    public static class _ProjectService2DocumentAbstract extends _Project2DocumentAbstract
    {
        protected _ProjectService2DocumentAbstract(final String _uuid)
        {
            super(_uuid);
        }
        public final CIAttribute FromService = new CIAttribute(this, "FromService");
        public final CIAttribute ToDocument = new CIAttribute(this, "ToDocument");
    }
}
