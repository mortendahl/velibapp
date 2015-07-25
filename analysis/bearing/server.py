import cherrypy
import json
from datetime import datetime, date, timedelta


class Simple(object):
    exposed = True
    
    @cherrypy.tools.accept(media='application/json')
    def GET(self, **params):
        request = cherrypy.request
        events = ( json.loads(line) for line in open('eventstore.json', 'rt') )
        events = filter(lambda e: e.get('class', '') == 'LocationUpdateEvent', events)
        events = filter(lambda e: 'bearing' in e, events)
        min_timestamp = int(datetime.fromordinal((date.today() - timedelta(days=2)).toordinal()).timestamp() * 1000)
        events = filter(lambda e: min_timestamp <= e.get('timestamp'), events)
        response = json.dumps(list(events))
        return response


if __name__ == '__main__':
    
    conf = {
        '/': {
            'request.dispatch': cherrypy.dispatch.MethodDispatcher(),
            'tools.sessions.on': True,
            'tools.response_headers.on': True,
            'tools.response_headers.headers': [('Content-Type', 'text/json'), ('Access-Control-Allow-Origin', '*')]  # unsafe
        }
    }
    
    cherrypy.tree.mount(Simple(), '/points', conf)

#    cherrypy.server.socket_host = server_host
    cherrypy.engine.start()
    cherrypy.engine.block()
