package conan.ci.arg

import groovy.json.JsonBuilder
import org.jenkinsci.plugins.workflow.cps.CpsScript


class ArgumentList implements List<Argument> {
    CpsScript currentBuild
    List<Argument> arguments
    Map<String, String> asMap

    static ArgumentList construct(CpsScript currentBuild, List<Argument> arguments = null){
        def it = new ArgumentList(arguments)
        it.currentBuild = currentBuild
        return it
    }

    private ArgumentList(List<Argument> arguments = null) {
        this.arguments = arguments ?: []
    }

    void parseArgs(Map config) {
        arguments.each { Argument a ->
            a.reconcile(config?.get(a.name) ?: null, currentBuild)
        }
        asMap = arguments
                .findAll { it.reconciledValue != null }
                .collectEntries { Argument a ->
            return [(a.name): a.toString()]
        }
    }

    @Override
    String toString() {
        return new JsonBuilder(asMap).toPrettyString()
    }

    String help() {
        List<String> outList = []
        outList.add(formatRow("Argument Name  |", "Environment Variable Name  |", "Description"))
        outList.add(formatRow("=" * 20, "=" * 20, "=" * 20))
        outList.addAll(arguments.collect { formatRow(it.name, it.envVarName, it.description) })
        return outList.join("\n")
    }

    static String formatRow(String s1, String s2, String s3) {
        List<String> l1 = s1.findAll(~/.{1,30}/)
        List<String> l2 = s2.findAll(~/.{1,30}/)
        List<String> l3 = s3.findAll(~/.{1,30}/)
        int col1width = 30
        int col2width = 90
        List<String> merged = (0..(Math.max(l1.size(), l2.size()))).collect { i ->
            String value = ""
            int l1length = 0
            int col2pad
            int col3pad
            if (i < l1.size()) {
                value += l1[i]
                l1length = l1[i].length()
                col2pad = col1width - l1length
            } else {
                col2pad = col1width
            }
            if (i < l2.size()) {
                value += l2[i].padLeft(col2pad)
                col3pad = col1width
            } else {
                col3pad = col1width + col2width - l1length
            }
            if (i < l3.size()) {
                value += l3[i].padLeft(col3pad)
            }
            return value
        }
        return merged.join("\n")
    }


    @Override
    List<Argument> subList(int fromIndex, int toIndex) {
        return arguments[fromIndex..toIndex]
    }

    @Override
    ListIterator<Argument> listIterator() {
        return arguments.listIterator()
    }

    @Override
    ListIterator<Argument> listIterator(int index) {
        return arguments.listIterator(index)
    }


    @Override
    int size() {
        return arguments.size()
    }

    @Override
    boolean isEmpty() {
        return arguments.isEmpty()
    }

    @Override
    boolean contains(Object o) {
        return arguments.contains(o)
    }

    @Override
    Iterator<Argument> iterator() {
        return arguments.iterator()
    }

    @Override
    Object[] toArray() {
        return arguments.toArray()
    }

    @Override
    def <T> T[] toArray(T[] a) {
        return arguments.toArray(a)
    }

    @Override
    boolean add(Argument argument) {
        return false
    }

    @Override
    boolean remove(Object o) {
        return arguments.remove(o)
    }

    @Override
    boolean containsAll(Collection<?> c) {
        return arguments.containsAll(c)
    }

    @Override
    boolean addAll(Collection<? extends Argument> c) {
        return arguments.addAll(c)
    }

    @Override
    boolean addAll(int index, Collection<? extends Argument> c) {
        return arguments.addAll(index, c)
    }

    @Override
    boolean removeAll(Collection<?> c) {
        return arguments.removeAll(c)
    }

    @Override
    boolean retainAll(Collection<?> c) {
        return arguments.retainAll(c)
    }

    @Override
    void clear() {
        arguments.clear()
    }

    @Override
    Argument get(int index) {
        arguments.get(index)
    }

    @Override
    Argument set(int index, Argument element) {
        return arguments.set(index, element)
    }

    @Override
    void add(int index, Argument element) {
        arguments.add(index, element)
    }

    @Override
    Argument remove(int index) {
        return arguments.remove(index)
    }

    @Override
    int indexOf(Object o) {
        return arguments.indexOf(o)
    }

    @Override
    int lastIndexOf(Object obj) {
        return arguments.lastIndexOf(obj)
    }

}
