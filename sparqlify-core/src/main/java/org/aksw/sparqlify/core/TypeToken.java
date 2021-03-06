package org.aksw.sparqlify.core;

import java.util.HashMap;
import java.util.Map;



/**
 * A class for wrapping strings that act as type names.
 *
 *
 *
 * @author Claus Stadler <cstadler@informatik.uni-leipzig.de>
 *
 */
public class TypeToken
{
    private static final Map<String, TypeToken> cache = new HashMap<String, TypeToken>();

    public static TypeToken alloc(String name) {
        if(name == null) {
            throw new NullPointerException();
        }

        TypeToken result = cache.get(name);
        if(result == null) {
            result = new TypeToken(name);
            cache.put(name, result);
        }
        return result;
    }

    /*
     * Some common datatype tokens
     */
    public static final TypeToken Object = alloc("object");

    public static final TypeToken Numeric = alloc("numeric");

    public static final TypeToken Byte = alloc("byte");
    public static final TypeToken Boolean = alloc("boolean");
    public static final TypeToken Int = alloc("integer");
    public static final TypeToken Long = alloc("long");
    public static final TypeToken Float = alloc("float");
    public static final TypeToken Double = alloc("double");
    public static final TypeToken String = alloc("string");

    public static final TypeToken Date = alloc("date");
    public static final TypeToken DateTime = alloc("dateTime");

    public static final TypeToken TypeError = alloc("type_error");
    public static final TypeToken Special = alloc("special");


    // For convenience, we also cover additional datatypes here
    public static final TypeToken Geometry = TypeToken.alloc("geometry");



    @Deprecated // Should not be used, because SqlExpr objects don't treat rdfTerm as a datatype
    public static final TypeToken rdfTerm = alloc("rdfTerm");

    private String name;

    public TypeToken(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TypeToken other = (TypeToken) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }
}
