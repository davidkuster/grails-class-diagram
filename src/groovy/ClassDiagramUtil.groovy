import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsControllerClass
import org.codehaus.groovy.grails.commons.GrailsServiceClass
import java.util.regex.Pattern

/**
 * Utility methods moved from ClassDiagramService
 */
class ClassDiagramUtil {

    static String getPackageName(cls) {
        // Name of root package is inconsistent
        if (cls instanceof GrailsDomainClass
            || cls instanceof GrailsControllerClass
            || cls instanceof GrailsServiceClass) {
            cls.packageName == "" ? "<root>" : cls.packageName
        } else {
            cls.package?.name ?: "<root>"
        }
    }

    /**
     * Order package names according to preferences
     */
    static Collection randomizeOrder(coll, ClassDiagramPreferences prefs) {
        if (prefs.randomizeOrder) {
            def list = coll as List
            Collections.shuffle(list)
            return list
        } else {
            return coll
        }
    }

    /**
     * find subset of classes according to preferences
     */
    static Collection classSelection(classList, ClassDiagramPreferences prefs) {
        if (!prefs.classSelection || prefs.classSelection == "<all>") {
            return classList
        }
        if (prefs.classSelectionIsRegexp) {
            matchClassesByRegexp(classList, prefs.classSelection)
        } else {
            matchClassesByName(classList, prefs.classSelection)
        }
    }

    /**
     *
     */
    static Collection excludeSelection(classList, ClassDiagramPreferences prefs) {
        def classesToExclude = []
        if (prefs.classExcludeSelection) {
            if (prefs.classExcludeSelectionIsRegexp) {
                classesToExclude = matchClassesByRegexp(classList, prefs.classExcludeSelection)
            }
            else {
                classesToExclude = matchClassesByName(classList, prefs.classExcludeSelection)
            }
        }
        classesToExclude
    }

    /**
     * finds classes that match a given regular expression
     */
    static Collection matchClassesByRegexp(classList, regexp) {
        def pattern = Pattern.compile(addRegexpWildcardsWhereNeeded(regexp))
        classList.findAll { cls ->
            String fullName = cls.packageName + "." + cls.name
            fullName ==~ pattern
        }
    }

    /**
     * finds classes that match a given string
     */
    static Collection matchClassesByName(classList, name) {
        classList.findAll { cls ->
            String fullName = cls.packageName + "." + cls.name
            fullName.indexOf(stripWildcardsOnEnds(name)) >= 0
        }
    }

    /*
     * strip preceeding and succeeding wildcards (*) from string
     */
    private static String stripWildcardsOnEnds(String s) {
        if (s.startsWith('*')) {
            s = s[1..-1]
        }
        if (s.endsWith('*')) {
            s = s[0..-2]
        }
        s
    }

    /*
     * Add regexp wildcards (.*) before and after s, and before and after every '|' (regexp or).
     */
    private static String addRegexpWildcardsWhereNeeded(String s) {
        s.tokenize('|').collect {
            def sb = new StringBuilder()
            if (!it.startsWith('.*')) {
                sb.append(".*")
            }
            sb.append(it)
            if (!it.endsWith('.*')) {
                sb.append(".*")
            }
            sb.toString()
        }.join('|')
    }


    // There may be a String method for this, but I didnt find it :)
    static String initCap(String s) {
        s ? s[0].toUpperCase() + (s.size() > 1 ? s[1..-1] : '') : s
    }

    /**
     * @returns true if the methods has the same signature, without looking at the class name.
     */
    static boolean hasSameSignature(method1, method2)  {
        if (method1?.name != method2?.name) {
            return false
        }
        if (method1.parameterTypes != method2.parameterTypes) {
            return false
        }
        return method1.returnType == method2.returnType
    }


    static String formatEnumProperty(property, ClassDiagramPreferences prefs) {
        if (property.type != property.declaringClass) {
            formatProperty(property, prefs)
        } else {
            property.name
        }
    }

    static String formatProperty(property, ClassDiagramPreferences prefs) {
        if (prefs.showPropertyType) {
            property.type.simpleName+" "+property.name
        } else {
            property.name
        }
    }

    static String formatMethod(method, ClassDiagramPreferences prefs) {
        def returnType = prefs.showMethodReturnType ? method.returnType.simpleName + " " : ""
        def methodSignature = prefs.showMethodSignature ? method.parameterTypes.collect{it.simpleName}.join(',') : ""
        returnType + method.name+"(${methodSignature})"
    }

}