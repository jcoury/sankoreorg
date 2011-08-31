/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

package com.xpn.xwiki.objects;

import com.xpn.xwiki.web.Utils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import java.io.Serializable;

/**
 * Base class for representing an element having a name (either a reference of a free form name) and a pretty name.
 *
 * @version $Id$
 */
public abstract class BaseElement implements ElementInterface, Serializable
{
    /**
     * Reference to the document in which this element is defined (for elements where this make sense, for example
     * for an XClass or a XObject).
     */
    private DocumentReference reference;

    /**
     * Free form name (for elements which don't point to a reference, for example for instances of
     * {@link BaseProperty}).
     */
    private String name;

    private String prettyName;

    /**
     * Used to convert a proper Document Reference to a string but without the wiki name.
     */
    private EntityReferenceSerializer<String> localEntityReferenceSerializer =
        Utils.getComponent(EntityReferenceSerializer.class, "local");

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.objects.ElementInterface#getDocumentReference()
     * @since 2.2M2
     */
    public DocumentReference getDocumentReference()
    {
        // Object using name without setting a reference are not allowed to retrieve the reference
        if (this.reference == null && this.name != null) {
            throw new IllegalStateException(
                "BaseElement#getDocumentReference could not be called when a non-reference Name has been set.");
        }

        return this.reference;
    }

    /**
     * Note that this method is used by Hibernate for saving an element.
     *
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.objects.ElementInterface#getName()
     */
    public String getName()
    {
        // If the name is null then serialize the reference as a string.
        if (this.name == null && this.reference != null) {
            this.name = this.localEntityReferenceSerializer.serialize(this.reference);
        }

        return this.name;
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.objects.ElementInterface#setDocumentReference(DocumentReference)
     * @since 2.2M2
     */
    public void setDocumentReference(DocumentReference reference)
    {
        // If the name is already set then reset it since we're now using a reference
        this.reference = reference;
        this.name = null;
    }

    /**
     * Note that this method is used by Hibernate for loading an element.
     *
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.objects.ElementInterface#setName(java.lang.String)
     */
    public void setName(String name)
    {
        // If a reference is already set, then you cannot set a name
        if (this.reference != null) {
            throw new IllegalStateException("BaseElement#setName could not be called when a reference has been set.");
        }

        this.name = name;
    }

    public String getPrettyName()
    {
        return this.prettyName;
    }

    public void setPrettyName(String name)
    {
        this.prettyName = name;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object el)
    {
        // Same Java object, they sure are equal
        if (this == el) {
            return true;
        }

        if (el == null || !(el.getClass().equals(this.getClass()))) {
            return false;
        }

        BaseElement element = (BaseElement) el;

        if (element.reference != null) {
            if (!element.reference.equals(this.reference)) {
                return false;
            }
        } else {
            if (this.reference != null) {
                return false;
            }
            if (element.name == null) {
                if (this.name != null) {
                    return false;
                }
            } else if (!element.name.equals(this.name)) {
                return false;
            }
        }

        if (element.getPrettyName() == null) {
            if (getPrettyName() != null) {
                return false;
            }
        } else if (!element.getPrettyName().equals(getPrettyName())) {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone()
    {
        BaseElement element;
        try {
            element = getClass().newInstance();

            // Make sure we clone either the reference or the name depending on which one is used.
            if (this.reference != null) {
                element.setDocumentReference(getDocumentReference());
            } else if (this.name != null) {
                element.setName(getName());
            }

            element.setPrettyName(getPrettyName());
        } catch (Exception e) {
            // This should not happen
            element = null;
        }

        return element;
    }
}
