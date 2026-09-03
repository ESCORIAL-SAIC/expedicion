package com.expedicion.app.data.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiUrlValidatorTest {

    @Test
    fun `url vacia es invalida`() {
        val resultado = ApiUrlValidator.validate("")
        assertTrue(resultado is ApiUrlValidationResult.Invalid)
    }

    @Test
    fun `url solo con espacios es invalida`() {
        val resultado = ApiUrlValidator.validate("   ")
        assertTrue(resultado is ApiUrlValidationResult.Invalid)
    }

    @Test
    fun `url sin esquema http o https es invalida`() {
        val resultado = ApiUrlValidator.validate("10.0.2.2:3000")
        assertTrue(resultado is ApiUrlValidationResult.Invalid)
    }

    @Test
    fun `url con esquema ftp es invalida`() {
        val resultado = ApiUrlValidator.validate("ftp://10.0.2.2:3000/")
        assertTrue(resultado is ApiUrlValidationResult.Invalid)
    }

    @Test
    fun `url http valida se acepta y normaliza con barra final`() {
        val resultado = ApiUrlValidator.validate("http://10.0.2.2:3000")
        assertEquals(ApiUrlValidationResult.Valid("http://10.0.2.2:3000/"), resultado)
    }

    @Test
    fun `url https valida ya con barra final no se modifica`() {
        val resultado = ApiUrlValidator.validate("https://api.miempresa.com/")
        assertEquals(ApiUrlValidationResult.Valid("https://api.miempresa.com/"), resultado)
    }

    @Test
    fun `url con espacios alrededor se recorta antes de validar`() {
        val resultado = ApiUrlValidator.validate("  http://10.0.2.2:3000/  ")
        assertEquals(ApiUrlValidationResult.Valid("http://10.0.2.2:3000/"), resultado)
    }

    @Test
    fun `url con esquema pero sin host es invalida`() {
        val resultado = ApiUrlValidator.validate("http://")
        assertTrue(resultado is ApiUrlValidationResult.Invalid)
    }

    @Test
    fun `url con esquema en mayusculas se acepta`() {
        val resultado = ApiUrlValidator.validate("HTTP://10.0.2.2:3000")
        assertTrue(resultado is ApiUrlValidationResult.Valid)
    }

    @Test
    fun `url con esquema en mayusculas se normaliza a minusculas en la url normalizada`() {
        val resultado = ApiUrlValidator.validate("HTTP://10.0.2.2:3000") as ApiUrlValidationResult.Valid
        assertEquals("http://10.0.2.2:3000/", resultado.normalizedUrl)
    }

    @Test
    fun `url con espacio en el medio del host es invalida`() {
        val resultado = ApiUrlValidator.validate("http://10.0.2.2 3000/")
        assertTrue(resultado is ApiUrlValidationResult.Invalid)
    }

    @Test
    fun `url con puerto y sin path se acepta y normaliza con barra final`() {
        val resultado = ApiUrlValidator.validate("http://10.0.2.2:3000")
        assertEquals(ApiUrlValidationResult.Valid("http://10.0.2.2:3000/"), resultado)
    }

    @Test
    fun `url con query string sin barra final normaliza agregando la barra antes del query`() {
        val resultado = ApiUrlValidator.validate("http://10.0.2.2:3000/api?token=abc") as ApiUrlValidationResult.Valid
        assertEquals("http://10.0.2.2:3000/api/?token=abc", resultado.normalizedUrl)
    }

    @Test
    fun `esquema vacio con doble barra es invalido`() {
        val resultado = ApiUrlValidator.validate("//10.0.2.2:3000/")
        assertTrue(resultado is ApiUrlValidationResult.Invalid)
    }
}
