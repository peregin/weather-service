import React from 'react';
import {
  BrowserRouter, Route, Routes,
} from 'react-router-dom';

import { ChakraProvider } from '@chakra-ui/react'

import Home from "./pages/Home"
import Best from "./pages/Best"
import About from "./pages/About"

import Header from './components/Header'
import Footer from './components/Footer'

import './App.css'

const App = () => {

  return (
    <ChakraProvider>
      <BrowserRouter>

          <Routes>
            <Route exact path="/" element={<div><Header /><Home /><Footer /></div>} />
            <Route exact path="/best" element={<Best />} />
            <Route path="/about" element={<div><Header /><About /><Footer /></div>} />
          </Routes>

      </BrowserRouter>
    </ChakraProvider>
  )
}

export default App;
