s = new XmlSlurper()
def r = s.parseText(new File('PM_getServices_resp.xml').text)

r.message_body.configure.cell_datas.children().each { cell_data ->
println cell_data.'@id'
}
