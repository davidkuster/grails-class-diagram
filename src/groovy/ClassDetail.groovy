
abstract class ClassDetail {

    abstract String getNodeName()

    abstract List getInterestingAssociations()
    abstract List getSubClasses()
    abstract List getInterestingProperties()
    abstract List getInterestingMethods()

    /**
     * Get methods declared in a class, filtering out all inherited and meta-added stuff.
     * Quite a few assumptions are made, no satisfactory solution found. Hack!
     * The Class.getDeclaredMethods() also includes decorated methods, which makes it essentially useless.
     * I think we need a grails getUndecoratedDeclaredMethods() that gives us what we really coded in the class, if that is possible.
     */
    protected getDeclaredMethods(methods, propertyNames) {
        def filterMethods = methods.findAll { it.name =~ /\$/} // remove special methods containing $
        filterMethods += GroovyObject.methods.flatten() // remove metaClass, properties etc.
        filterMethods += Object.methods.flatten() // remove toString

        commonsMethods.each { methodtoremove ->
            filterMethods += methods.findAll {it.name =~ methodtoremove}
        }

        // filter out property-related methods
        methods.each { method ->
            ["get","is","set","addTo","removeFrom"].each { prefix ->
                propertyNames.each{ propertyName ->
                    if (method.name == prefix + ClassDiagramUtil.initCap(propertyName)) {
                        // TODO: Filter by signature, not just name
                        filterMethods += method
                    }
                }
            }
        }

        // Apply filter
        filterMethods.each {filterMethod ->
            methods -= methods.find { ClassDiagramUtil.hasSameSignature(it, filterMethod)}
        }

        return methods
    }

}