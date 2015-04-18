require('chai/chai').should()
global.$ = require('lib/jquery/jquery')
describe '$', ->
  it 'is function', ->
    global.$.should.be.a('function')
