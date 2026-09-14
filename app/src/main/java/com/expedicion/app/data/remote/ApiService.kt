package com.expedicion.app.data.remote

import com.expedicion.app.data.remote.dto.CredencialesRequest
import com.expedicion.app.data.remote.dto.DetalleRemitoResponseDto
import com.expedicion.app.data.remote.dto.EliminarEtiquetaRequest
import com.expedicion.app.data.remote.dto.EliminarResponseDto
import com.expedicion.app.data.remote.dto.EscaneoRequest
import com.expedicion.app.data.remote.dto.EstadoRequest
import com.expedicion.app.data.remote.dto.EstadoResponseDto
import com.expedicion.app.data.remote.dto.LoginResponseDto
import com.expedicion.app.data.remote.dto.RemitoListResponseDto
import com.expedicion.app.data.remote.dto.ScanResponseDto
import com.expedicion.app.data.remote.dto.SuccessResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contrato 1:1 con las rutas de `api/src/http/routes`. Las credenciales viajan tanto en el body (para
 * los endpoints que lo requieren) como via header Authorization: Basic (inyectado por
 * AuthInterceptor para TODOS los requests, body-based o no) — el servidor acepta ambos modos
 * (ver resolveCredentials en api/src/http/middlewares/auth.ts) y aca se mandan los dos de forma
 * consistente para no depender de cual gana.
 *
 * Nota tecnica: Retrofit no permite @Body en @DELETE directamente (asume metodos sin cuerpo);
 * se usa @HTTP(method = "DELETE", hasBody = true) para los DELETE que llevan body.
 */
interface ApiService {

    @POST("auth/login")
    suspend fun login(@Body body: CredencialesRequest): Response<LoginResponseDto>

    @GET("remitos/despacho")
    suspend fun listarRemitosDespacho(@Query("remitoN") remitoN: String): Response<RemitoListResponseDto>

    @GET("remitos/devolucion")
    suspend fun listarRemitosDevolucion(@Query("remitoN") remitoN: String): Response<RemitoListResponseDto>

    // `tipo` acota los items a los productos de ese tipo dentro del remito; vacio trae el remito
    // completo.
    @GET("remitos/{remitoId}/detalle")
    suspend fun detalleRemito(
        @Path("remitoId") remitoId: String,
        @Query("esDespacho") esDespacho: Boolean,
        @Query("tipo") tipo: String = "",
    ): Response<DetalleRemitoResponseDto>

    @POST("despacho/{remitoId}/escaneo")
    suspend fun escanearDespacho(
        @Path("remitoId") remitoId: String,
        @Body body: EscaneoRequest,
    ): Response<ScanResponseDto>

    @POST("devolucion/{remitoId}/escaneo")
    suspend fun escanearDevolucion(
        @Path("remitoId") remitoId: String,
        @Body body: EscaneoRequest,
    ): Response<ScanResponseDto>

    @HTTP(method = "DELETE", path = "despacho/{remitoId}/etiqueta", hasBody = true)
    suspend fun eliminarEtiquetaDespacho(
        @Path("remitoId") remitoId: String,
        @Body body: EliminarEtiquetaRequest,
    ): Response<EliminarResponseDto>

    @HTTP(method = "DELETE", path = "devolucion/{remitoId}/etiqueta", hasBody = true)
    suspend fun eliminarEtiquetaDevolucion(
        @Path("remitoId") remitoId: String,
        @Body body: EliminarEtiquetaRequest,
    ): Response<EliminarResponseDto>

    @HTTP(method = "DELETE", path = "despacho/{remitoId}/transaccion", hasBody = true)
    suspend fun borrarTransaccionDespacho(
        @Path("remitoId") remitoId: String,
        @Body body: CredencialesRequest,
    ): Response<SuccessResponseDto>

    @HTTP(method = "DELETE", path = "devolucion/{remitoId}/transaccion", hasBody = true)
    suspend fun borrarTransaccionDevolucion(
        @Path("remitoId") remitoId: String,
        @Body body: CredencialesRequest,
    ): Response<SuccessResponseDto>

    @POST("despacho/{remitoId}/confirmar")
    suspend fun confirmarDespacho(
        @Path("remitoId") remitoId: String,
        @Body body: CredencialesRequest,
    ): Response<SuccessResponseDto>

    @POST("devolucion/{remitoId}/confirmar")
    suspend fun confirmarDevolucion(
        @Path("remitoId") remitoId: String,
        @Body body: CredencialesRequest,
    ): Response<SuccessResponseDto>

    @POST("etiquetas/estado")
    suspend fun consultarEstado(@Body body: EstadoRequest): Response<EstadoResponseDto>

    // Circuitos IMPORT / PEABODY. El slug va como @Path y no interpolado en la anotacion porque
    // Retrofit exige que el path sea una constante de compilacion; el servidor registra los dos
    // slugs como literales (ver api/src/http/routes/circuitos.ts), asi que un slug desconocido
    // da 404 y no cae en ningun catch-all.
    @GET("remitos/{circuito}")
    suspend fun listarRemitosCircuito(
        @Path("circuito") circuito: String,
        @Query("remitoN") remitoN: String,
    ): Response<RemitoListResponseDto>

    @POST("{circuito}/{remitoId}/escaneo")
    suspend fun escanearCircuito(
        @Path("circuito") circuito: String,
        @Path("remitoId") remitoId: String,
        @Body body: EscaneoRequest,
    ): Response<ScanResponseDto>

    @HTTP(method = "DELETE", path = "{circuito}/{remitoId}/etiqueta", hasBody = true)
    suspend fun eliminarEtiquetaCircuito(
        @Path("circuito") circuito: String,
        @Path("remitoId") remitoId: String,
        @Body body: EliminarEtiquetaRequest,
    ): Response<EliminarResponseDto>

    @HTTP(method = "DELETE", path = "{circuito}/{remitoId}/transaccion", hasBody = true)
    suspend fun borrarTransaccionCircuito(
        @Path("circuito") circuito: String,
        @Path("remitoId") remitoId: String,
        @Body body: CredencialesRequest,
    ): Response<SuccessResponseDto>

    @POST("{circuito}/{remitoId}/confirmar")
    suspend fun confirmarCircuito(
        @Path("circuito") circuito: String,
        @Path("remitoId") remitoId: String,
        @Body body: CredencialesRequest,
    ): Response<SuccessResponseDto>
}
