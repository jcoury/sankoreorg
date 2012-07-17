package org.xwiki.sankore.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

@Component
@Named("UserProfileClass")
@Singleton
public class UserProfileClass implements ClassManager<UserProfileObjectDocument>
{
    public static final String FIELD_PROFILE = "profile";
    public static final String FIELDPN_PROFILE = "User profile";

    public static final String FIELD_ALLOW_NOTIFICATIONS = "allowNotifications";
    public static final String FIELDPN_ALLOW_NOTIFICATIONS = "Allow notifications";

    public static final String FIELD_ALLOW_NOTIFICATIONS_FROM_SELF = "allowNotificationsFromSelf";
    public static final String FIELDPN_ALLOW_NOTIFICATIONS_FROM_SELF = "Allow Notifications From Self";

    @Inject
    private Logger logger;

    @Inject
    Execution execution;

    @Inject
    @Named("current/reference")
    DocumentReferenceResolver<EntityReference> currentReferenceDocumentReferenceResolver;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> referenceSerializer;

    private EntityReference classReference = new EntityReference("UserProfileClass", EntityType.DOCUMENT,
            new EntityReference("XWiki", EntityType.SPACE));

    private EntityReference sheetReference = new EntityReference("UserProfileSheet", EntityType.DOCUMENT,
            new EntityReference("XWiki", EntityType.SPACE));

    private EntityReference templateReference = new EntityReference("UserProfileTemplate", EntityType.DOCUMENT,
            new EntityReference("XWiki", EntityType.SPACE));

    private XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }

    public UserProfileObjectDocument getDocumentObject(DocumentReference documentReference, int objectId)
            throws XWikiException
    {
        return getDocumentObject(documentReference);
    }

    public UserProfileObjectDocument getDocumentObject(DocumentReference documentReference)
            throws XWikiException
    {
        XWikiContext context = getContext();
        DefaultXObjectDocumentClass<UserProfileObjectDocument> cls =
                new DefaultXObjectDocumentClass<UserProfileObjectDocument>(getClassDocumentReference(), context);
        XWikiDocument doc = context.getWiki().getDocument(documentReference, context);
        BaseObject obj = doc.getXObject(getClassDocumentReference());

        if (obj == null) {
            return null;
        }

        return new UserProfileObjectDocument(cls, doc, obj, context);
    }

    public UserProfileObjectDocument newDocumentObject(DocumentReference documentReference)
            throws XWikiException
    {
        XWikiContext context = getContext();
        DefaultXObjectDocumentClass<UserProfileObjectDocument> cls =
                new DefaultXObjectDocumentClass<UserProfileObjectDocument>(getClassDocumentReference(), context);
        XWikiDocument doc = context.getWiki().getDocument(documentReference, context);
        BaseObject obj = doc.getXObject(getClassDocumentReference(), true, context);

        if (obj == null)
            return null;

        return new UserProfileObjectDocument(cls, doc, obj, context);
    }

    public DocumentReference getClassDocumentReference()
    {
        return currentReferenceDocumentReferenceResolver.resolve(classReference);
    }

    public DocumentReference getClassSheetDocumentReference()
    {
        return currentReferenceDocumentReferenceResolver.resolve(sheetReference);
    }

    public DocumentReference getClassTemplateDocumentReference()
    {
        return currentReferenceDocumentReferenceResolver.resolve(templateReference);
    }

    public List<UserProfileObjectDocument> searchDocumentObjectsByField(String fieldName, Object fieldValue)
            throws XWikiException
    {
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(fieldName, fieldValue);

        return searchDocumentObjectsByFields(fields);
    }

    public List<UserProfileObjectDocument> searchDocumentObjectsByFields(Map<String, Object> fields) throws XWikiException
    {
        String from = "select distinct doc.space, doc.name, obj.number from XWikiDocument as doc, BaseObject as obj";
        String where = " where obj.name=doc.fullName and obj.className='XWiki.UserProfileClass'";
        BaseClass baseClass = getContext().getWiki().getXClass(getClassDocumentReference(), getContext());
        List<PropertyClass> enabledProperties = baseClass.getEnabledProperties();

        int i = 0;
        String propName = StringUtils.EMPTY;
        String propType = StringUtils.EMPTY;
        String propValue = StringUtils.EMPTY;
        for (PropertyClass propertyClass : enabledProperties) {
            if (fields.keySet().contains(propertyClass.getName())) {
                propName = "prop" + Integer.toString(i);
                propType =  propertyClass.newProperty().getClass().getSimpleName();
                propValue = fields.get(propertyClass.getName()).toString();
                from = from.concat(", " + propType + " as " + propName);
                where = where.concat(" and " + propName + ".id.id=obj.id and " + propName + ".name='" + propertyClass.getName() + "'");
                if (StringUtils.equals(propType, "IntegerProperty")) {
                    if (StringUtils.equals(propValue, "true"))
                        where = where.concat(" and " + propName + ".value>0");
                    else
                        where = where.concat(" and " + propName + ".value=0");
                } else if (StringUtils.equals(propType, "LongProperty")
                        || StringUtils.equals(propType, "FloatProperty")
                        || StringUtils.equals(propType, "DoubleProperty")) {
                    where = where.concat(" and " + propName + ".value=" + fields.get(propertyClass.getName()).toString());
                } else if (StringUtils.equals(propType, "StringProperty")
                        || StringUtils.equals(propType, "LargeStringProperty")
                        || StringUtils.equals(propType, "DateProperty")) {
                    where = where.concat(" and " + propName + ".value='" + fields.get(propertyClass.getName()).toString() + "'");
                } else if (StringUtils.equals(propType, "StringListProperty")) {
                    where = where.concat(" and " + propName + ".textValue='" + fields.get(propertyClass.getName()).toString() + "'");
                } else if (StringUtils.equals(propType, "DBStringListProperty")) {
                    from = from.concat(" join " + propName + ".list " + propName + "list");
                    where = where.concat(" and " + propName + "list='" + fields.get(propertyClass.getName()).toString() + "'");
                }
                i++;
            }
        }

        List<UserProfileObjectDocument> userProfileObjectDocuments = new ArrayList<UserProfileObjectDocument>();
        List<Object> results = getContext().getWiki().getStore().search(from + where, 0, 0, getContext());
        String docSpace = StringUtils.EMPTY;
        String docName = StringUtils.EMPTY;
        int objNumber = 0;
        for (Object result : results) {
            Object[] resultParams = (Object[]) result;
            docSpace = resultParams[0].toString();
            docName = resultParams[1].toString();
            objNumber = Integer.parseInt(resultParams[2].toString());
            DocumentReference documentReference = currentReferenceDocumentReferenceResolver.resolve(
                    new EntityReference(docName, EntityType.DOCUMENT, new EntityReference(docSpace, EntityType.SPACE)));
            userProfileObjectDocuments.add(getDocumentObject(documentReference, objNumber));
        }

        return userProfileObjectDocuments;
    }

    public void saveDocumentObject(UserProfileObjectDocument documentObject) throws XWikiException
    {
        //documentObject.save();
        documentObject.saveDocument("UserProfileObjectDocument saved.", false);
    }

    @Override
    public String toString()
    {
        return referenceSerializer.serialize(this.classReference);
    }
}
