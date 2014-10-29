#!/usr/bin/env python
# -*- coding: utf-8 -*-
#
# One-Stop-Shop CLI tool for interacting with Sarcasmotron

import argparse
import socket
import httplib2
import datetime
import humanize
import readline

from json import loads, dumps

__version__ = '0.0.1'

response_encoding = "utf-8"


def make_parser():
    parser = argparse.ArgumentParser(description='One-Stop-Shop CLI tool for interacting with Sarcasmotron')
    parser.add_argument('-v', '--version', help='Program version', action='version', version='%(prog)s ' + __version__)
    parser.add_argument('--host', help='Sarcasmotron host', default='localhost')
    parser.add_argument('-p', '--port', help='Sarcasmotron port', default=8080, type=int)
    parser.add_argument('-s', '--sarcasms', help='Print the Sarcasms?', action='store_true', default=False)
    parser.add_argument('-o', '--order', help='In what order are we printing? ("timestamp", '
                                              '"quote", "context", "id", "user", "creator", "voteTotal")', default='timestamp')
    parser.add_argument('-r', '--reverse', help='Reverse the order of printing?', action='store_true', default=False)
    parser.add_argument('-c', '--create', help='Create a sarcasm', action='store_true', default=False)
    parser.add_argument('-u', '--upvote', help='Upvote a sarcasm', action='store_true', default=False)
    parser.add_argument('-d', '--downvote', help='Downvote a sarcasm', action='store_true', default=False)
    parser.add_argument('-t', '--trend', help='Trending data!', action='store_true', default=False)
    parser.add_argument('-x', '--stats', help='Statistics!', action='store_true', default=False)
    return parser


def request_dict_from_sarcasmotron(host=None, port=None, context=None, data=None, method=None):
    h = httplib2.Http()
    dicti = None
    try:
        resp, content = h.request(
        uri='http://' + host + ':' + str(port) + context,
        method=method, headers={'Content-Type': 'application/json'},
        body=dumps(data))
        content_decode = content.decode(response_encoding)
        if content_decode:
            dicti = loads(content_decode)
    except socket.error:
        resp = {'message': "Server cannot be found!"}
    return resp, dicti


def print_sarcasms(arguments):
    resp, sarcasms = request_dict_from_sarcasmotron(
            host=arguments.host, port=arguments.port,
            context='/sarcasms', method='GET')
    if resp.get('message', 0) == 0:
        if sarcasms is not None and sarcasms.get('_embedded', 0) != 0:
            sarcasm_list = (sarcasms['_embedded']['sarcasms'])
            sarcasm_list = sorted(sarcasm_list, key=lambda k: k[arguments.order], reverse=arguments.reverse)
            for sarcasm in sarcasm_list:
                timestamp = datetime.datetime.strptime(sarcasm['timestamp'].split("+")[0], '%Y-%m-%dT%H:%M:%S.%f')
                print("Sarcasm ID:      " + sarcasm['id'])
                print("     Timestamp:  " + humanize.naturaltime(timestamp))
                print("     User:       " + sarcasm['user'])
                print("     Quote:      " + sarcasm['quote'])
                print("     Context:    " + sarcasm['context'])
                print("     Creator:    " + sarcasm['creator'])
                print("     Votes:      " + str(sarcasm['voteTotal']))
                print("")
        else:
            print("No sarcasms available!")
    else:
        print(resp['message'])


def main():
    parser = make_parser()
    arguments = parser.parse_args()
    if arguments.sarcasms:
        print_sarcasms(arguments)
    elif arguments.create:
        user = raw_input("About who do you wanna report a sarcasm? ")
        quote = raw_input("What did he say? ")
        context = raw_input("In which context? ")
        now = datetime.datetime.now()
        sarcasm = {'timestamp': now.strftime('%Y-%m-%dT%H:%M:%S.%f')[:-3] + "+0100", 'user': user,
                   'creator': "gert", 'quote': quote, 'context': context}
        created, resp = request_dict_from_sarcasmotron(host=arguments.host, port=arguments.port, context='/sarcasms',
                                                      method='POST', data=sarcasm)
        print("Code: " + created['status'] + " - ID: " + created['location'].split("/")[-1])
    elif arguments.upvote:
        sid = raw_input("What is the id of the sarcasm? ")
        sarcasm_id = {'sarcasmId': sid}
        voted, resp = request_dict_from_sarcasmotron(host=arguments.host, port=arguments.port, context='/upvote', method='POST', data=sarcasm_id)
        print("Code: " + voted['status'] + " - Message: " + resp['message'])
    elif arguments.downvote:
        sid = raw_input("What is the id of the sarcasm? ")
        sarcasm_id = {'sarcasmId': sid}
        voted, resp = request_dict_from_sarcasmotron(host=arguments.host, port=arguments.port, context='/downvote', method='POST', data=sarcasm_id)
        print("Code: " + voted['status'] + " - Message: " + resp['message'])
    elif arguments.trend:
        user = raw_input("Whose trend-line you wanna see? ")
        interval = raw_input("Interval in days or weeks? ('1d' or '2w', ...) ")
        period = raw_input("For what period in days or weeks? ('5d' or '3w, ...) ")
        request = {'user': user, 'intervalExpression': interval, 'periodExpression': period}
        trend, resp = request_dict_from_sarcasmotron(host=arguments.host, port=arguments.port, context='/trend',
                                                      method='POST', data=request)
        if resp['message'] is not None:
            print("Code: " + trend['status'] + " - Response: " + resp['message'])
        else:
            for x in resp:
                if resp[x] is not None:
                    for y in resp[x]:
                        print (str(y) + ' -> ' + str(resp[x][y]))
    elif arguments.stats:
        user = raw_input("Whose statistics you wanna see? (or '*' for everyone) ")
        period = raw_input("For what period in days or weeks? ('5d' or '3w, ...) ")
        request = {'user': user, 'periodExpression': period}
        stats, resp = request_dict_from_sarcasmotron(host=arguments.host, port=arguments.port, context='/votestats',
                                                      method='POST', data=request)
        print("Code: " + stats['status'] + " - Response: " + resp['message'])
        if resp.get('voteStats', 0) != 0:
            for x in resp['voteStats']:
                if resp['voteStats'][x] is not None:
                    print("User: " + str(x))
                    for key, value in sorted(resp['voteStats'][x].items()):
                        print(" -> {} : {}".format(key, value))
    else:
        print_sarcasms(arguments)

if __name__ == '__main__':
    main()